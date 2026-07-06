---
title: Future - Component RIMs
---

<style>
/* ==========================================================================
   REUSABLE GRADIENT CLASS
   ========================================================================== */
.high-tech-border {
  border-collapse: separate !important; /* Required for tables */
  border-spacing: 0 !important;          /* Required for tables */
  border-radius: 12px !important;       /* Clean rounded corners */
  overflow: hidden !important;          /* Keeps inner content from bleeding out */
  background-color: var(--md-default-bg-color) !important; /* Adaptive light/dark fill */
  
  /* The High-Tech Blue Gradient Edge */
  border: 2px solid transparent !important;
  background-image: linear-gradient(var(--md-default-bg-color), var(--md-default-bg-color)), 
                    linear-gradient(to right, #0052cc, #4ba3e3) !important;
  background-origin: border-box !important;
  background-clip: padding-box, border-box !important;
  
  /* Modern soft shadow depth */
  box-shadow: 0 10px 25px -5px rgba(0, 82, 204, 0.08), 
              0 8px 10px -6px rgba(0, 82, 204, 0.08) !important;
}

/* ==========================================================================
   CORE TABLE FUNCTIONAL STYLES (Keep these intact)
   ========================================================================== */

/* 2. Target the exact table cells directly to blindside the theme override */
.md-typeset div.md-typeset__table table.fancy-table th,
.md-typeset div.md-typeset__table table.fancy-table td {
  /* This prevents the theme's default bottom/top gray lines from mixing with yours */
  border-top: none !important; 
  border-bottom: none !important;
  padding: 12px 16px !important;
}

.md-typeset div.md-typeset__table table.fancy-table th.gray-bottom-line-thick,
.md-typeset div.md-typeset__table table.fancy-table td.gray-bottom-line-thick {
  border-bottom: 2px solid #808080 !important; 
}

.md-typeset div.md-typeset__table table.fancy-table th.gray-bottom-line-thin,
.md-typeset div.md-typeset__table table.fancy-table td.gray-bottom-line-thin {
  border-bottom: 1px solid #808080 !important; 
}

.md-typeset div.md-typeset__table table.fancy-table th.gray-right-line-thick,
.md-typeset div.md-typeset__table table.fancy-table td.gray-right-line-thick {
  border-right: 2px solid #808080 !important;
}

.md-typeset div.md-typeset__table table.fancy-table th.gray-right-line-thin,
.md-typeset div.md-typeset__table table.fancy-table td.gray-right-line-thin {
  border-right: 1px solid #808080 !important;
}

/* 3. GLOBAL TABLE LIST OVERRIDES - This fixes ALL lists in this table */
.md-typeset div.md-typeset__table table.fancy-table ul {
  margin: 0 !important;
  padding-left: 0px !important;
  list-style-type: disc !important;
}

.md-typeset div.md-typeset__table table.fancy-table li {
  margin-top: 1px !important;
  margin-bottom: 1px !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  line-height: 1.3 !important;
}

</style>

# Component RIMs

Component Reference Integrity Manifests (RIMs) are cryptographically signed data structures that provide 
a secure baseline of expected firmware and software measurements for hardware components. 
In other words, a Component RIM is a generic term for an object that holds "golden"
expected measurements for a component.
During computer attestation, validation services use these trusted manufacturer assertions to verify that 
a device's actual boot state remains secure, unmodified, and free from compromise.

!!! note

    Compenent RIM processing is currently in progress for HIRS attestation services, but is not fully
    developed yet.

Component RIMs are similar in purpose to the [PC Client RIM](rim.md), but specifically designed for 
computer components. Unlike a PC Client RIM which typically exists as a single standardized metadata 
format - the SWID (Software Identification) tag - Component RIMs come in many different formats and typically 
use Concise Binary Object Representation (CBOR) encoding and CBOR Object Signing and Encryption (COSE) 
signatures. 

The following table shows the relationship between some of the common formats:

<div class="md-typeset__scrollwrap">
  <div class="md-typeset__table">
    <table class="fancy-table high-tech-border">
      <thead>
        <tr>
          <th class="gray-right-line-thick gray-bottom-line-thick">Encoding</th>
          <th class="gray-bottom-line-thick gray-right-line-thin">Format</th>
          <th class="gray-bottom-line-thick">Description</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td class="gray-right-line-thick gray-bottom-line-thin"><strong>Traditional XML Encoding (ISO / TCG)</strong></td>
          <td class="gray-bottom-line-thin gray-right-line-thin">TCG Component RIM SWID</td>
          <td class="gray-bottom-line-thin">
            <ul>
              <li>Top-level signed XML envelope</li>
              <li>Contains software/firmware data blocks formatted as SWID tags</li>
            </ul>
          </td>
        </tr>
        <tr>
          <td class="gray-right-line-thick"><strong></strong></td>
          <td class="gray-bottom-line-thin gray-right-line-thin">IETF CoSWID</td>
          <td class="gray-bottom-line-thin">
            <ul>
              <li>Top-level signed CBOR envelope</li>
            </ul>
          </td>
        </tr>
        <tr>
          <td class="gray-right-line-thick"><strong>Modern Concise CBOR Encoding (IETF / RATS)</strong></td>
          <td class="gray-bottom-line-thin gray-right-line-thin">TCG Component RIM CoSWID</td>
          <td class="gray-bottom-line-thin">
            <ul>
              <li>Top-level signed CBOR envelope</li>
            </ul>
          </td>
        </tr>
        <tr>
          <td class="gray-right-line-thick"><strong></strong></td>
          <td class="gray-right-line-thin">IETF CoRIM</td>
          <td>
            <ul>
              <li>Top-level signed CBOR envelope</li>
              <li>Payload = unsigned-corim-map structure</li>
              <li>Subcomponent options inside the unsigned-corim-map</li>
                <ul style="padding-left: 25px !important;">
                  <li>IETF CoSWID</li>
                  <li>TCG Component RIM CoSWID</li>
                  <li>IETF CoMID</li>
                </ul>
            </ul>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</div>

## IETF CoSWID vs TCG Component RIM CoSWID

A TCG Component RIM CoSWID is an augmented, specialized flavor of an IETF CoSWID.
The IETF CoSWID is the base blueprint: It is an industry-wide, generic CBOR structure designed to 
define any software or firmware footprint of a component. The TCG CoSWID is the extension: The Trusted 
Computing Group (TCG) took that exact IETF CoSWID map and used built-in CBOR extension points to add 
specialized hardware attributes (like specific SPDM platform registers or hardware vendor IDs).

## IETF CoRIM Structure

Since the CoRIM structure is complex, below is a diagram to help visualize it:

<div class="high-tech-border" style="white-space: pre-wrap !important; padding: 12px;">
    📦 Signed IETF CoRIM (COSE Envelope)
    └── 📑 PAYLOAD: unsigned-corim-map
         ├── 🆔 corim.id (Manifest UUID)
         └── 🗂️ corim.tags [ Array of Child Tags ]
              ├── 🧩 Tag 1: IETF CoMID (hardware and reference values)
              ├── 📜 Tag 2: IETF CoSWID (software manifests and cryptographic hashes)
              └── 🏷️ Tag 3: TCG Component RIM CoSWID (TCG-specific CBOR mapping)
</div>