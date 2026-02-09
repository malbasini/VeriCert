package com.example.vericert.enumerazioni;
/*
ADMIN: gestione tenant, utenti, ruoli, template, tutto.

MANAGER: vede tutto, emette/revoca certificati, gestisce template ma non utenti.

ISSUER: emette certificati, niente ruoli/template.

VIEWER: sola lettura.
*/
public enum Role { ADMIN, MANAGER, ISSUER, VIEWER }
