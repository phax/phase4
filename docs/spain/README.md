# Spain - Mandatory B2B e-invoicing (SEFE / SPFE)

Local copy of the primary sources for the Spanish mandatory B2B e-invoicing system,
collected for GitHub issue [#398](https://github.com/phax/phase4/issues/398).

Everything below is derived from the documents stored in this folder, not from secondary sources.

## Why this is relevant for phase4

The Spanish system is a two-tier model:

| Tier | Who | Transport |
|---|---|---|
| Public hub - **SPFE** (*Solucion Publica de Facturacion Electronica*), run by AEAT | anybody; mandatory recipient of "faithful copies" of all invoices | synchronous **web services** (WSDL on the AEAT Sede electronica) + web form, authenticated with electronic certificates. **Not AS4.** |
| **Private platforms** (*plataformas privadas de intercambio de facturas electronicas*) | commercial e-invoicing service providers | RD 238/2026 Art. 13.1.b): must use secure transmission protocols "complying with the **AS2 or AS4** specifications" |

So AS4 in Spain is relevant **only for private-platform to private-platform interconnection**.
See [AS4-relevance.md](AS4-relevance.md) for the detailed analysis and the list of open questions.

## Timeline (as of 2026-09-10)

* 2026-03-31 - RD 238/2026 published in the BOE
* 2026-04-20 - RD 238/2026 in force (20 days after publication, DF cuarta.1)
* 2026-04-17 - draft Ministerial Order (SPFE) opened for public consultation
* 2026-05-19 - AEAT developer seminar on the SPFE
* 2026-09-10 - AEAT webinar "Actualizacion sobre la SPFE" (no documents published on the page at the time of writing)
* 2026-10-01 - planned entry into force of the Ministerial Order (draft, DF unica); starts all deadlines
* 2026-10-01 (reference date, per the AEAT technical deck) - SPFE test environment availability, not before the Order is published in the BOE
* +12 months - obligations for private platforms (RD Arts. 6, 8, 9 and **13**) and for taxpayers with turnover > EUR 8 million (DF cuarta.1.a and .2)
* +24 months - obligations for all remaining businesses and professionals (DF cuarta.1.b)

The draft Order was still in public consultation, so all dates that depend on it may still move.

## Contents

| Folder | Contents |
|---|---|
| [legal/](legal/) | RD 238/2026 (BOE) and the draft Ministerial Order on the SPFE, PDF plus extracted plain text |
| [aeat/2026-05-19-seminario-spfe/](aeat/2026-05-19-seminario-spfe/) | The four documents of the AEAT developer seminar of 2026-05-19 |

## Source pages

* AEAT developer portal - https://www.agenciatributaria.es/AEAT.desarrolladores/Desarrolladores/Desarrolladores.html
* Seminar 2026-05-19 - https://www.agenciatributaria.es/AEAT.desarrolladores/Desarrolladores/_menu_/Reuniones/Ejercicio_2026/Seminario_SPFE/Seminario_SPFE.html
* Webinar 2026-09-10 - https://www.agenciatributaria.es/AEAT.desarrolladores/Desarrolladores/_menu_/Reuniones/Ejercicio_2026/Actualizacion_sobre_la_Solucion_Publica_de_Facturacion_Electronica__SPFE_/Actualizacion_sobre_la_Solucion_Publica_de_Facturacion_Electronica__SPFE_.html
* Technical questions to AEAT: facturaelectronica@correo.aeat.es
