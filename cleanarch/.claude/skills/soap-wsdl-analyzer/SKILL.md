---
name: soap-wsdl-analyzer
description: >
  Analyze a SOAP WSDL endpoint or file and generate sample XML request and response for every operation.
  Use this skill whenever the user provides a WSDL URL or file and wants to understand the XML format,
  generate sample requests/responses, or explore available operations. Triggers include: "analyze WSDL",
  "generate SOAP request", "WSDL sample XML", "what operations does this WSDL have", "SOAP service structure",
  "generate request response from WSDL", or any time a WSDL URL/file is mentioned alongside XML or SOAP context.
  Always use this skill when the user pastes or mentions a .wsdl file or a URL ending in ?wsdl.
---

# SOAP WSDL Analyzer Skill

Fetch, parse, and generate sample XML request/response for every operation in a WSDL.

---

## Input

The user provides one of:
- A WSDL URL (e.g. `http://example.com/service?wsdl`)
- A local `.wsdl` file path
- Raw WSDL XML pasted in the conversation

---

## Step-by-Step Workflow

### Step 1 — Fetch or read the WSDL

**If URL:**
```bash
curl -s "http://example.com/service?wsdl" -o /tmp/service.wsdl
```

**If local file:** copy to `/tmp/service.wsdl`

**If pasted XML:** write directly to `/tmp/service.wsdl`

---

### Step 2 — Parse the WSDL with Python

Run this script to extract all operations + their request/response element names:

```python
import xml.etree.ElementTree as ET

NS = {
    'wsdl': 'http://schemas.xmlsoap.org/wsdl/',
    'soap': 'http://schemas.xmlsoap.org/wsdl/soap/',
    'soap12': 'http://schemas.xmlsoap.org/wsdl/soap12/',
    'xsd':  'http://www.w3.org/2001/XMLSchema',
}

tree = ET.parse('/tmp/service.wsdl')
root = tree.getroot()

# Detect targetNamespace
tns = root.get('targetNamespace', '')

# --- Collect XSD types ---
def collect_types(root):
    """Returns dict: element_name -> {field: type}"""
    types = {}
    for schema in root.findall('.//{http://www.w3.org/2001/XMLSchema}schema'):
        for elem in schema.findall('{http://www.w3.org/2001/XMLSchema}element'):
            name = elem.get('name')
            fields = {}
            ct = elem.find('{http://www.w3.org/2001/XMLSchema}complexType')
            if ct is not None:
                seq = ct.find('.//{http://www.w3.org/2001/XMLSchema}sequence')
                if seq is not None:
                    for child in seq:
                        fname = child.get('name', '?')
                        ftype = child.get('type', 'xs:string').split(':')[-1]
                        fmin  = child.get('minOccurs', '1')
                        fmax  = child.get('maxOccurs', '1')
                        fields[fname] = {'type': ftype, 'min': fmin, 'max': fmax}
            types[name] = fields
    return types

# --- Collect messages ---
def collect_messages(root):
    msgs = {}
    for msg in root.findall('wsdl:message', NS):
        name = msg.get('name')
        for part in msg.findall('wsdl:part', NS):
            elem = part.get('element', '')
            msgs[name] = elem.split(':')[-1]  # strip ns prefix
    return msgs

# --- Collect operations ---
def collect_operations(root, messages):
    ops = []
    for pt in root.findall('wsdl:portType', NS):
        for op in pt.findall('wsdl:operation', NS):
            op_name = op.get('name')
            inp  = op.find('wsdl:input',  NS)
            outp = op.find('wsdl:output', NS)
            inp_msg  = inp.get('message',  '').split(':')[-1] if inp  is not None else None
            outp_msg = outp.get('message', '').split(':')[-1] if outp is not None else None
            ops.append({
                'name':         op_name,
                'req_element':  messages.get(inp_msg),
                'resp_element': messages.get(outp_msg),
            })
    return ops

# --- Collect SOAP actions ---
def collect_soap_actions(root):
    actions = {}
    for binding in root.findall('wsdl:binding', NS):
        for op in binding.findall('wsdl:operation', NS):
            name = op.get('name')
            soap_op = op.find('soap:operation', NS)
            if soap_op is None:
                soap_op = op.find('soap12:operation', NS)
            if soap_op is not None:
                actions[name] = soap_op.get('soapAction', '')
    return actions

types    = collect_types(root)
messages = collect_messages(root)
ops      = collect_operations(root, messages)
actions  = collect_soap_actions(root)

import json
print(json.dumps({
    'targetNamespace': tns,
    'operations': ops,
    'types': types,
    'soapActions': actions,
}, indent=2))
```

---

### Step 3 — Generate sample XML for each operation

For each operation, build the SOAP Envelope using the parsed structure.

**XML generation rules:**
- Wrap everything in `<soapenv:Envelope>` + `<soapenv:Body>`
- Use `targetNamespace` as `xmlns:tns`
- For each field, use the field name as XML tag; fill value with `[field_name:type]` placeholder
- If `maxOccurs="unbounded"`, show the element twice with comment `<!-- repeat as needed -->`
- If `minOccurs="0"`, add comment `<!-- optional -->`

**Sample output format:**

```
════════════════════════════════════════
Operation: GetUser
SOAPAction: urn:GetUser
════════════════════════════════════════

▶ REQUEST
──────────────────────────────────────
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:tns="http://example.com/ns">
  <soapenv:Header/>
  <soapenv:Body>
    <tns:GetUserRequest>
      <tns:userId>[userId:string]</tns:userId>
      <tns:includeDetails>[includeDetails:boolean]</tns:includeDetails>
    </tns:GetUserRequest>
  </soapenv:Body>
</soapenv:Envelope>

▶ RESPONSE
──────────────────────────────────────
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:tns="http://example.com/ns">
  <soapenv:Body>
    <tns:GetUserResponse>
      <tns:userName>[userName:string]</tns:userName>
      <tns:email>[email:string]</tns:email>
      <tns:age>[age:int]</tns:age>
    </tns:GetUserResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

---

### Step 4 — Output summary table

After all operations, print a summary:

```
┌─────────────────┬──────────────────────┬───────────────────────┐
│ Operation       │ Request Element      │ Response Element      │
├─────────────────┼──────────────────────┼───────────────────────┤
│ GetUser         │ GetUserRequest       │ GetUserResponse       │
│ CreateUser      │ CreateUserRequest    │ CreateUserResponse    │
└─────────────────┴──────────────────────┴───────────────────────┘
Endpoint: http://example.com/service
Total operations: 2
```

---

## Edge Cases

| Situation | Handling |
|-----------|----------|
| `<part type=...>` instead of `element=` | Resolve the named `complexType` directly from `<types>` |
| Imported XSD (`<xsd:import schemaLocation=...>`) | Fetch and merge the external XSD before parsing |
| SOAP 1.2 binding | Use `xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope/"` |
| `<xs:extension base=...>` inheritance | Merge parent fields into child fields |
| No `<types>` section (bare WSDL) | Show only operation names, note that schema is missing |
| Auth-protected WSDL | Ask user to paste raw XML directly |

---

## Output Delivery

- Print all operations to the conversation (inline code blocks)
- If more than 5 operations exist, also save to `/mnt/user-data/outputs/wsdl-analysis.md` and present the file
- Always end with the summary table