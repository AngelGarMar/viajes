package com.transporte.viajes.services.impl;

import com.transporte.viajes.clients.*;
import com.transporte.viajes.constants.Constants;
import com.transporte.viajes.dto.ViajeRequest;
import com.transporte.viajes.dto.ViajeResponse;
import com.transporte.viajes.entities.Boleto;
import com.transporte.viajes.entities.Pago;
import com.transporte.viajes.entities.Tipo;
import com.transporte.viajes.entities.Viaje;
import com.transporte.viajes.enums.PagoStatus;
import com.transporte.viajes.models.*;
import com.transporte.viajes.repositories.BoletoRepository;
import com.transporte.viajes.repositories.PagoRepository;
import com.transporte.viajes.repositories.TipoRepository;
import com.transporte.viajes.repositories.ViajeRepository;
import com.transporte.viajes.services.ViajeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ViajeServiceImpl implements ViajeService {
    @Autowired
    private ViajeRepository viajeRepository;
    @Autowired
    private TipoRepository tipoRepository;
    @Autowired
    private BoletoRepository boletoRepository;
    @Autowired
    private PagoRepository pagoRepository;

    @Value("${app.maintenance.createViaje}")
    private boolean createViaje;

    @Autowired
    private final ParaderosClient paraderosClient;
    @Autowired
    private final UsuarioClient usuarioClient;
    @Autowired
    private final ClienteClient clienteClient;
    @Autowired
    private final ConductorClient conductorClient;
    @Autowired
    private final GetParaderoClient getParaderoClient;

    public ViajeServiceImpl(ParaderosClient paraderosClient, UsuarioClient usuarioClient, ClienteClient clienteClient,
                            ConductorClient conductorClient, GetParaderoClient getParaderoClient) {
        this.paraderosClient = paraderosClient;
        this.usuarioClient = usuarioClient;
        this.clienteClient = clienteClient;
        this.conductorClient = conductorClient;
        this.getParaderoClient = getParaderoClient;
    }

    @Override
    @Transactional
    public ResponseService createViaje(ViajeRequest request) {
        log.info("se revisa si por configuración se puede crear el viaje");
        if (!createViaje) {
            log.info("no se puede crea el viaje");
            return new ResponseService(Constants.RESPONSE_TYPE_WARNING, Constants.CREATE_VIAJE_DISABLED, null);
        }
        log.info("se revisa que existan el el origen y destino"); // Mediante WebClient
        ParaderosRequest paraderosRequest = new ParaderosRequest();
        paraderosRequest.setOrigenId(request.getOrigenId());
        paraderosRequest.setDestinoId(request.getDestinoId());
        ResponseService responseParaderos = paraderosClient.existsParaderos(paraderosRequest);
        if (!responseParaderos.getResponseType().equals(Constants.RESPONSE_TYPE_SUCCESS)) {
            log.info("algún paradero no existe");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.PARADEROS_UNKNOWN, null);
        }
        log.info("se revisa que el usuario exista");
        ResponseService responseUsuario = usuarioClient.getUser(request.getUsuarioId());
        if (!responseUsuario.getResponseType().equals(Constants.RESPONSE_TYPE_SUCCESS)) {
            log.info("el usuario no existe");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.USER_UNKNOWN, null);
        }
        log.info("se revisa que el cliente exista");
        ResponseService responseCliente = clienteClient.getCliente(request.getClienteId());
        if (!responseCliente.getResponseType().equals(Constants.RESPONSE_TYPE_SUCCESS)) {
            log.info("el cliente no existe");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.CLIENT_UNKNOWN, null);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        Cliente cliente = objectMapper.convertValue(responseCliente.getData(), Cliente.class);
        log.info("se revisa que el conductor exista");
        ResponseService responseConductor = conductorClient.getConductor(request.getConductorId());
        if (!responseConductor.getResponseType().equals(Constants.RESPONSE_TYPE_SUCCESS)) {
            log.info("no existe el conductor");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.DRIVER_UNKNOWN, null);
        }
        Conductor conductor = objectMapper.convertValue(responseConductor.getData(), Conductor.class);
        log.info("se revisa que los lugares sean mayores o iguales a cero");
        if (request.getLugaresAdulto() < 0 || request.getLugaresMenor() < 0 || request.getLugaresPaqueteria() < 0 ||
        request.getLugaresAdultoMayor() < 0 || request.getLugaresCortesia() < 0) {
            log.info("existe un lugar con número negativo, no puede ser");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.PLACES_INVALID, null);
        }
        int lugaresTotales = request.getLugaresAdulto() + request.getLugaresMenor() + request.getLugaresPaqueteria() +
                request.getLugaresAdultoMayor() + request.getLugaresCortesia();
        if (lugaresTotales <= 0) {
            log.info("la suma de los lugares es menor o igual a cero, no existen lugares a apartar");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.PLACES_INVALID, null);
        }
        log.info("se comienza a guardar el viaje");
        Viaje viaje = Viaje.builder()
                .origenId(request.getOrigenId())
                .destinoId(request.getDestinoId())
                .lugares(lugaresTotales)
                .creado(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Mexico_City"))))
                .estatus(1)
                .usuarioId(request.getUsuarioId())
                .clienteId(request.getClienteId())
                .conductorId(request.getConductorId())
                .build();
        Viaje viajeSaved = viajeRepository.save(viaje);
        log.info("se comienzan a guardar los boletos");
        int numero = 0;
        double pagoTotal = 0.0;
        Optional<Tipo> tipoOptional = tipoRepository.findById(1); // 1 = Adulto
        if (tipoOptional.isEmpty()) {
            log.info("no existe el tipo de boleto adulto");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.PLACE_TYPE_UNKNOWN, null);
        }
        Tipo tipoAdulto = tipoOptional.get();
        pagoTotal = pagoTotal + (tipoAdulto.getCosto() * request.getLugaresAdulto());
        for (int i = 0 ; i < request.getLugaresAdulto() ; i++) {
            Boleto boleto = Boleto.builder()
                    .numero(++numero)
                    .creado(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Mexico_City"))))
                    .estatus(1)
                    .viaje(viajeSaved)
                    .tipo(tipoAdulto)
                    .build();
            boletoRepository.save(boleto);
        }
        tipoOptional = tipoRepository.findById(2); // 2 = Menor
        if (tipoOptional.isEmpty()) {
            log.info("no existe el tipo de boleto menor");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.PLACE_TYPE_UNKNOWN, null);
        }
        Tipo tipoMenor = tipoOptional.get();
        pagoTotal = pagoTotal + (tipoMenor.getCosto() * request.getLugaresMenor());
        for (int i = 0 ; i < request.getLugaresMenor() ; i++) {
            Boleto boleto = Boleto.builder()
                    .numero(++numero)
                    .creado(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Mexico_City"))))
                    .estatus(1)
                    .viaje(viajeSaved)
                    .tipo(tipoMenor)
                    .build();
            boletoRepository.save(boleto);
        }
        tipoOptional = tipoRepository.findById(3); // 3 = Paquetería
        if (tipoOptional.isEmpty()) {
            log.info("no existe el tipo de boleto paquetería");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.PLACE_TYPE_UNKNOWN, null);
        }
        Tipo tipoPaqueteria = tipoOptional.get();
        pagoTotal = pagoTotal + (tipoPaqueteria.getCosto() * request.getLugaresPaqueteria());
        for (int i = 0 ; i < request.getLugaresPaqueteria() ; i++) {
            Boleto boleto = Boleto.builder()
                    .numero(++numero)
                    .creado(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Mexico_City"))))
                    .estatus(1)
                    .viaje(viajeSaved)
                    .tipo(tipoPaqueteria)
                    .build();
            boletoRepository.save(boleto);
        }
        tipoOptional = tipoRepository.findById(4); // 4 = Adulto Mayor
        if (tipoOptional.isEmpty()) {
            log.info("no existe el tipo de boleto adulto mayor");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.PLACE_TYPE_UNKNOWN, null);
        }
        Tipo tipoMayor = tipoOptional.get();
        pagoTotal = pagoTotal + (tipoMayor.getCosto() * request.getLugaresAdultoMayor());
        for (int i = 0 ; i < request.getLugaresAdultoMayor() ; i++) {
            Boleto boleto = Boleto.builder()
                    .numero(++numero)
                    .creado(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Mexico_City"))))
                    .estatus(1)
                    .viaje(viajeSaved)
                    .tipo(tipoMayor)
                    .build();
            boletoRepository.save(boleto);
        }
        tipoOptional = tipoRepository.findById(5); // 5 = Cortesía
        if (tipoOptional.isEmpty()) {
            log.info("no existe el tipo de boleto cortesia");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.PLACE_TYPE_UNKNOWN, null);
        }
        Tipo tipoCortesia = tipoOptional.get();
        pagoTotal = pagoTotal + (tipoCortesia.getCosto() * request.getLugaresCortesia());
        for (int i = 0 ; i < request.getLugaresCortesia() ; i++) {
            Boleto boleto = Boleto.builder()
                    .numero(++numero)
                    .creado(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Mexico_City"))))
                    .estatus(1)
                    .viaje(viajeSaved)
                    .tipo(tipoCortesia)
                    .build();
            boletoRepository.save(boleto);
        }
        log.info("se comienza a guardar el registro del pago, aun no autorizado");
        Pago pago = Pago.builder()
                .estatus(PagoStatus.CREADO)
                .creado(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Mexico_City"))))
                .costo(pagoTotal)
                .viaje(viajeSaved)
                .build();
        pagoRepository.save(pago);
        log.info("se regresa el viaje creado");
        ViajeResponse response = new ViajeResponse();
        response.setId(viajeSaved.getId());
        response.setOrigen(responseParaderos.getData().toString().split(";")[0]);
        response.setDestino(responseParaderos.getData().toString().split(";")[1]);
        response.setLugares(lugaresTotales);
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(viajeSaved.getCreado().getTime()), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        response.setCreado(dateTime.format(formatter));
        response.setCliente(cliente.getNombre() + " " + cliente.getApaterno() + " " + cliente.getAmaterno());
        response.setConductor(conductor.getNombre() + " " + conductor.getApaterno() + " " + conductor.getAmaterno());
        response.setEstatus(pago.getEstatus());
        response.setCosto(pagoTotal);
        log.info("se mandan los lugares al microservicio de pagos para poder realizar el pago");
        return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.VIAJE_CREATED, response);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseService getAllViajes() {
        log.info("se comienzan a extraer todos los viajes");
        List<Viaje> viajes = viajeRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        if (viajes.isEmpty()) {
            log.info("no existen viajes a mostrar");
            return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.GET_TRAVELS, viajes);
        }
        log.info("se comienza a crear la lista para mostrar los viajes");
        ObjectMapper objectMapper = new ObjectMapper();
        List<ViajeResponse> response = new ArrayList<>();
        viajes.forEach(viaje -> {
            ViajeResponse viajeResponse = new ViajeResponse();
            viajeResponse.setId(viaje.getId());
            Paradero paraderoOrigen = objectMapper.convertValue(getParaderoClient.getParadero(viaje.getOrigenId()).getData(), Paradero.class);
            viajeResponse.setOrigen(paraderoOrigen.getNombre());
            Paradero paraderoDestino = objectMapper.convertValue(getParaderoClient.getParadero(viaje.getDestinoId()).getData(), Paradero.class);
            viajeResponse.setDestino(paraderoDestino.getNombre());
            viajeResponse.setLugares(viaje.getLugares());
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(viaje.getCreado().getTime()), ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            viajeResponse.setCreado(dateTime.format(formatter));
            Cliente cliente = objectMapper.convertValue(clienteClient.getCliente(viaje.getClienteId()).getData(), Cliente.class);
            viajeResponse.setCliente(cliente.getNombre() + " " + cliente.getApaterno() + " " + cliente.getAmaterno());
            Conductor conductor = objectMapper.convertValue(conductorClient.getConductor(viaje.getConductorId()).getData(), Conductor.class);
            viajeResponse.setConductor(conductor.getNombre() + " " + conductor.getApaterno() + " " + conductor.getAmaterno());
            Pago pago = pagoRepository.getByViaje(viaje).get();
            viajeResponse.setEstatus(pago.getEstatus());
            viajeResponse.setCosto(pago.getCosto());
            response.add(viajeResponse);
        });
        log.info("se regresa la lista de viajes");
        return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.GET_TRAVELS, response);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseService getAllViajesCreados() {
        log.info("se comienzan a extraer todos los viajes creados");
        List<Viaje> viajes = viajeRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        if (viajes.isEmpty()) {
            log.info("no existen viajes a mostrar");
            return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.GET_TRAVELS, viajes);
        }
        log.info("se comienza a crear la lista para mostrar los viajes creados");
        ObjectMapper objectMapper = new ObjectMapper();
        List<ViajeResponse> response = new ArrayList<>();
        viajes.forEach(viaje -> {
            ViajeResponse viajeResponse = new ViajeResponse();
            Pago pago = pagoRepository.getByViaje(viaje).get();
            if (pago.getEstatus().equals(PagoStatus.CREADO)) {
                viajeResponse.setEstatus(pago.getEstatus());
                viajeResponse.setCosto(pago.getCosto());
                viajeResponse.setId(viaje.getId());
                Paradero paraderoOrigen = objectMapper.convertValue(getParaderoClient.getParadero(viaje.getOrigenId()).getData(), Paradero.class);
                viajeResponse.setOrigen(paraderoOrigen.getNombre());
                Paradero paraderoDestino = objectMapper.convertValue(getParaderoClient.getParadero(viaje.getDestinoId()).getData(), Paradero.class);
                viajeResponse.setDestino(paraderoDestino.getNombre());
                viajeResponse.setLugares(viaje.getLugares());
                LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(viaje.getCreado().getTime()), ZoneId.systemDefault());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                viajeResponse.setCreado(dateTime.format(formatter));
                Cliente cliente = objectMapper.convertValue(clienteClient.getCliente(viaje.getClienteId()).getData(), Cliente.class);
                viajeResponse.setCliente(cliente.getNombre() + " " + cliente.getApaterno() + " " + cliente.getAmaterno());
                Conductor conductor = objectMapper.convertValue(conductorClient.getConductor(viaje.getConductorId()).getData(), Conductor.class);
                viajeResponse.setConductor(conductor.getNombre() + " " + conductor.getApaterno() + " " + conductor.getAmaterno());
                response.add(viajeResponse);
            }
        });
        log.info("se regresa la lista de viajes creados");
        return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.GET_TRAVELS, response);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseService getAllViajesConfirmados() {
        log.info("se comienzan a extraer todos los viajes confirmados");
        List<Viaje> viajes = viajeRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        if (viajes.isEmpty()) {
            log.info("no existen viajes a mostrar");
            return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.GET_TRAVELS, viajes);
        }
        log.info("se comienza a crear la lista para mostrar los viajes confirmados");
        ObjectMapper objectMapper = new ObjectMapper();
        List<ViajeResponse> response = new ArrayList<>();
        viajes.forEach(viaje -> {
            ViajeResponse viajeResponse = new ViajeResponse();
            Pago pago = pagoRepository.getByViaje(viaje).get();
            if (pago.getEstatus().equals(PagoStatus.PAGO_CONFIRMADO)) {
                viajeResponse.setEstatus(pago.getEstatus());
                viajeResponse.setCosto(pago.getCosto());
                viajeResponse.setId(viaje.getId());
                Paradero paraderoOrigen = objectMapper.convertValue(getParaderoClient.getParadero(viaje.getOrigenId()).getData(), Paradero.class);
                viajeResponse.setOrigen(paraderoOrigen.getNombre());
                Paradero paraderoDestino = objectMapper.convertValue(getParaderoClient.getParadero(viaje.getDestinoId()).getData(), Paradero.class);
                viajeResponse.setDestino(paraderoDestino.getNombre());
                viajeResponse.setLugares(viaje.getLugares());
                LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(viaje.getCreado().getTime()), ZoneId.systemDefault());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                viajeResponse.setCreado(dateTime.format(formatter));
                Cliente cliente = objectMapper.convertValue(clienteClient.getCliente(viaje.getClienteId()).getData(), Cliente.class);
                viajeResponse.setCliente(cliente.getNombre() + " " + cliente.getApaterno() + " " + cliente.getAmaterno());
                Conductor conductor = objectMapper.convertValue(conductorClient.getConductor(viaje.getConductorId()).getData(), Conductor.class);
                viajeResponse.setConductor(conductor.getNombre() + " " + conductor.getApaterno() + " " + conductor.getAmaterno());
                response.add(viajeResponse);
            }
        });
        log.info("se regresa la lista de viajes confirmados");
        return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.GET_TRAVELS, response);
    }

    @Override
    public ResponseService getAllViajesCancelados() {
        log.info("se comienzan a extraer todos los viajes cancelados");
        List<Viaje> viajes = viajeRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        if (viajes.isEmpty()) {
            log.info("no existen viajes a mostrar");
            return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.GET_TRAVELS, viajes);
        }
        log.info("se comienza a crear la lista para mostrar los viajes cancelados");
        ObjectMapper objectMapper = new ObjectMapper();
        List<ViajeResponse> response = new ArrayList<>();
        viajes.forEach(viaje -> {
            ViajeResponse viajeResponse = new ViajeResponse();
            Pago pago = pagoRepository.getByViaje(viaje).get();
            if (pago.getEstatus().equals(PagoStatus.CANCELADO)) {
                viajeResponse.setEstatus(pago.getEstatus());
                viajeResponse.setCosto(pago.getCosto());
                viajeResponse.setId(viaje.getId());
                Paradero paraderoOrigen = objectMapper.convertValue(getParaderoClient.getParadero(viaje.getOrigenId()).getData(), Paradero.class);
                viajeResponse.setOrigen(paraderoOrigen.getNombre());
                Paradero paraderoDestino = objectMapper.convertValue(getParaderoClient.getParadero(viaje.getDestinoId()).getData(), Paradero.class);
                viajeResponse.setDestino(paraderoDestino.getNombre());
                viajeResponse.setLugares(viaje.getLugares());
                LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(viaje.getCreado().getTime()), ZoneId.systemDefault());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                viajeResponse.setCreado(dateTime.format(formatter));
                Cliente cliente = objectMapper.convertValue(clienteClient.getCliente(viaje.getClienteId()).getData(), Cliente.class);
                viajeResponse.setCliente(cliente.getNombre() + " " + cliente.getApaterno() + " " + cliente.getAmaterno());
                Conductor conductor = objectMapper.convertValue(conductorClient.getConductor(viaje.getConductorId()).getData(), Conductor.class);
                viajeResponse.setConductor(conductor.getNombre() + " " + conductor.getApaterno() + " " + conductor.getAmaterno());
                response.add(viajeResponse);
            }
        });
        log.info("se regresa la lista de viajes cancelados");
        return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.GET_TRAVELS, response);
    }
}
