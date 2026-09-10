# AEAT developer seminar on the SPFE - 2026-05-19

Source page:
https://www.agenciatributaria.es/AEAT.desarrolladores/Desarrolladores/_menu_/Reuniones/Ejercicio_2026/Seminario_SPFE/Seminario_SPFE.html

| File | Description |
|---|---|
| `Seminario_19_05_MEconomia.pdf` | Ministry of Economy, Trade and Enterprise - business case and system overview |
| `Seminario_19_05_2026_Gestion.pdf` | AEAT Tax Management Department - walkthrough of the draft Ministerial Order |
| `Seminario_19_05_2026_DIT.pdf` | AEAT IT Department - technical aspects: UBL, EN 16931, Schematron validation, web services, test environment |
| `FAQ_Seminario_SPFE.xlsx` | Questions and answers collected during the seminar |
| `*.txt` | Plain text extracted with `pdftotext -layout`, kept for searching. |

None of these documents mentions AS4, AS2 or ebMS. They describe the SPFE only, which is
accessed via synchronous web services and a web form, not via AS4.

Key technical points from the DIT deck:

* SPFE works exclusively with UBL; EN 16931:2026 based on UBL 2.5 is needed to express the
  Spanish requirements (rectifying invoices, retentions, suplidos, reductions, equivalence
  surcharge, large-company special regime).
* Validation: OASIS XSD plus Schematron; senders are expected to validate before sending.
  AEAT will run its own high performance implementation of the Schematron rules and will
  **not** offer an online validation service in production.
* Web services are synchronous and require authentication with an electronic certificate;
  available acting in own name, under power of attorney, and - for submission services only -
  as "colaborador social".
* Private platforms must batch their submissions and retrievals; invoice-by-invoice or
  customer-by-customer operation against the SPFE is explicitly not intended.
* Test environment reference date 2026-10-01, not before the Ministerial Order is published.
* To be published before the test deployment: service catalogue, submission standards,
  authentication and representation, limits, error list, WSDLs, XSDs of the Spanish extensions,
  Schematron files and message examples.
