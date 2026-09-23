# Spain - Mandatory B2B e-invoicing (SEFE / SPFE)

Local copy of the primary sources for the Spanish mandatory B2B e-invoicing system,
collected for GitHub issue [#398](https://github.com/phax/phase4/issues/398).

Everything below is derived from the documents stored in this folder, not from secondary sources.

Status: 2026-09-23.

## Why this is relevant for phase4

The Spanish system is a two-tier model:

| Tier | Who | Transport |
|---|---|---|
| Public hub - **SPFE** (*Solucion Publica de Facturacion Electronica*), run by AEAT | anybody; mandatory recipient of "faithful copies" of all invoices | synchronous **web services** (WSDLs announced, not yet published) + web form, authenticated with electronic certificates. Not AS4, but the envelope is genuine **ebMS 3.0** with eBCore party IDs and CID-referenced MIME payloads, presented by AEAT as "compatible AS4 / Peppol" and carrying no WS-Security at all. |
| **Private platforms** (*plataformas privadas de intercambio de facturas electronicas*) | commercial e-invoicing service providers | RD 238/2026 Art. 13.1.b): must use secure transmission protocols "complying with the **AS2 or AS4** specifications" |

The AS4 *obligation* applies only to private-platform to private-platform interconnection.
The SPFE leg is a separate, ebMS-shaped protocol of AEAT's own design.
See [AS4-relevance.md](AS4-relevance.md) for the detailed analysis and the list of open questions.

## Timeline

* 2026-03-31 - RD 238/2026 published in the BOE
* 2026-04-17 - draft Ministerial Order (SPFE) opened for public consultation
* 2026-04-20 - RD 238/2026 in force (20 days after publication, DF cuarta.1)
* 2026-05-08 - public consultation (audiencia e informacion publica) on the draft Order closed
* 2026-05-19 - AEAT developer seminar on the SPFE
* 2026-08-06 - EU notification procedure under Directive (EU) 2015/1535 closed
* 2026-09-10 - AEAT seminar "Actualizacion sobre la SPFE"; decks published afterwards
* 2026-10-01 - planned entry into force of the Ministerial Order; starts all deadlines
* 2026-10 (reference date) - SPFE test environment, not before the Order is published in the BOE
* 2027-10-01 - e-invoicing and payment reporting for turnover > EUR 8 m; obligations for private
  platforms (RD Arts. 6, 8, 9 and 13) take effect (RD: +12 months, DF cuarta.1.a and .2)
* 2028-10-01 - e-invoicing for the rest; payment reporting for incorporated SMEs (RD: +24 months)
* 2029-10-01 - payment reporting for the remaining small taxpayers

**As of 2026-09-23 the Ministerial Order has not been published in the BOE**, so every date that
depends on it can still move. The 2027-2029 dates come from the AEAT slides; the +12/+24 month
structure is the RD's own (DF cuarta).

## Contents

| Folder | Contents |
|---|---|
| [legal/](legal/) | RD 238/2026 (BOE) and the draft Ministerial Order on the SPFE, PDF plus extracted plain text |
| [aeat/2026-05-19-seminario-spfe/](aeat/2026-05-19-seminario-spfe/) | The four documents of the AEAT developer seminar of 2026-05-19 |
| [aeat/2026-09-10-actualizacion-spfe/](aeat/2026-09-10-actualizacion-spfe/) | The three decks of the AEAT seminar of 2026-09-10 - technical SPFE services incl. the ebXML message examples, updated draft Order, Anexo I changes |

`*.txt` files are `pdftotext -layout` extractions of the PDF next to them, kept for searching.

## Source pages

* AEAT developer portal - https://www.agenciatributaria.es/AEAT.desarrolladores/Desarrolladores/Desarrolladores.html
* Seminar 2026-05-19 - https://www.agenciatributaria.es/AEAT.desarrolladores/Desarrolladores/_menu_/Reuniones/Ejercicio_2026/Seminario_SPFE/Seminario_SPFE.html
* Seminar 2026-09-10, documents - https://www.agenciatributaria.es/AEAT.desarrolladores/Desarrolladores/_menu_/Reuniones/Ejercicio_2026/Seminario_actualizacion_sobre_la_Solucion_Publica_de_Facturacion_Electronica__SPFE____10_09_2026/Seminario_actualizacion_sobre_la_Solucion_Publica_de_Facturacion_Electronica__SPFE____10_09_2026.html
* Seminar 2026-09-10, announcement/registration only - https://www.agenciatributaria.es/AEAT.desarrolladores/Desarrolladores/_menu_/Reuniones/Ejercicio_2026/Actualizacion_sobre_la_Solucion_Publica_de_Facturacion_Electronica__SPFE_/Actualizacion_sobre_la_Solucion_Publica_de_Facturacion_Electronica__SPFE_.html
* Technical questions to AEAT: facturaelectronica@correo.aeat.es
