# AEAT seminar "Actualizacion sobre la SPFE" - 2026-09-10

Source page (documents):
https://www.agenciatributaria.es/AEAT.desarrolladores/Desarrolladores/_menu_/Reuniones/Ejercicio_2026/Seminario_actualizacion_sobre_la_Solucion_Publica_de_Facturacion_Electronica__SPFE____10_09_2026/Seminario_actualizacion_sobre_la_Solucion_Publica_de_Facturacion_Electronica__SPFE____10_09_2026.html

Note: the page that announced the webinar
(`.../Actualizacion_sobre_la_Solucion_Publica_de_Facturacion_Electronica__SPFE_/...`) only carries
the Zoom registration link. The decks were published on the separate page above.

| File | Description |
|---|---|
| `DIT_FE_Seminario_10_septiembre.pdf` | AEAT IT Department - technical information on the SPFE services, 45 slides: UBL documents used, unique invoice ID and localizador, why ebXML, service catalogue, and the ebXML/UBL message examples for every service |
| `Actualizacion_proyecto_OM_Seminario_10-09-2026.pdf` | AEAT Tax Management Department - walkthrough of the updated draft Ministerial Order (11 articles, 2 DA, 1 DF, 2 annexes) |
| `Novedades_AnexoI_Seminario_10-9-26.pdf` | Changes to Anexo I, i.e. the UBL invoice content |
| `*.txt` | Plain text extracted with `pdftotext -layout`, kept for searching. |

The XML message examples in the technical deck are **screenshots**, so they do not appear in the
`.txt` extraction; they were read from the rendered slides. The AS4-relevant content is analysed
in [../../AS4-relevance.md](../../AS4-relevance.md), section 2, with slide numbers referring to
the PDF pages of `DIT_FE_Seminario_10_septiembre.pdf`.

Key technical points of the DIT deck:

* The SPFE web services use an ebMS 3.0 (ebXML) envelope with eBCore party identifiers
  (`urn:oasis:names:tc:ebcore:partyid-type:iso6523:9920`), an empty SOAP body and CID-referenced
  MIME payloads - presented as "Compatible AS4 / Peppol", explicitly a "minimalist version
  without cryptographic signature". No WS-Security header appears in any example.
* Eight endpoints: `ws/SendInvoiceSOAP`, `ws/CancelInvoiceSOAP`, `ws/GetInvoicesSOAP` (query by
  ID), `ws/GetRegisteredInvoiceSOAP` (query by filter), `ws/DownloadInvoicesByIDSOAP`,
  `ws/DownloadInvoicesByLOCSOAP`, `ws/CustomerInvoiceEventsSOAP`, `ws/SupplierInvoiceEventsSOAP`.
* Service values are in a CEN namespace (`urn:cen.eu:en16931:*`), not a Spanish one.
* Results come back as UBL `DocumentStatus`: `200` / `206` (partially correct) / `400` at batch
  level, per-invoice status codes and SPFE error codes below that, plus the CSV of the acuse de
  recibo. The download response carries per-payload `statusCode` inside the ebMS `PartProperties`.
* The envelope examples are not consistent about the SOAP version - SOAP 1.2 for submission,
  SOAP 1.1 for cancellation and status changes.
* Test environment reference date October 2026, not before the Ministerial Order is published in
  the BOE. Service catalogue, submission standards, authentication and representation, WSDLs,
  XSDs, Schematron files, validation documents and message examples are announced for the
  developer portal before the test deployment; none of them were published as of 2026-09-23.
