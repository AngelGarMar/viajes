package com.transporte.viajes.controllers;

import com.transporte.viajes.constants.Constants;
import com.transporte.viajes.dto.ViajeRequest;
import com.transporte.viajes.models.ResponseService;
import com.transporte.viajes.services.ViajeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/viajes")
public class ViajeController {
    @Autowired
    private ViajeService service;

    @PostMapping
    public ResponseEntity<ResponseService> createViaje(@RequestBody @Valid ViajeRequest request, BindingResult result) {
        if (result.hasFieldErrors()) return validation(result);
        ResponseService response = service.createViaje(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/all")
    public ResponseEntity<ResponseService> getAllViajes() {
        ResponseService response = service.getAllViajes();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/created")
    public ResponseEntity<ResponseService> getViajesCreados() {
        ResponseService response = service.getAllViajesCreados();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/confirmed")
    public ResponseEntity<ResponseService> getViajesConfirmados() {
        ResponseService response = service.getAllViajesConfirmados();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/cancelled")
    public ResponseEntity<ResponseService> getViajesCancelados() {
        ResponseService response = service.getAllViajesCancelados();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private ResponseEntity<ResponseService> validation(BindingResult result) {
        Map<String, String> errors = new HashMap<>();
        result.getFieldErrors().forEach(err -> {
            errors.put(err.getField(), err.getDefaultMessage());
        });
        return new ResponseEntity<>(new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.MISSED_INFORMATION, errors), HttpStatus.OK);
    }
}
