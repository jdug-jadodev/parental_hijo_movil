"""Valida ejemplos y contraejemplos estructurales. No prueba autorización ni Android.
Requiere Python 3 y jsonschema (python -m pip install jsonschema).
"""
from __future__ import annotations
import copy
import json
from pathlib import Path
from jsonschema import Draft202012Validator

ROOT = Path(__file__).resolve().parents[1]
schema = json.loads((ROOT / 'compartido/protocolo.schema.json').read_text(encoding='utf-8'))
Draft202012Validator.check_schema(schema)
validator = Draft202012Validator(schema)
files = sorted((ROOT / 'compartido/ejemplos').glob('*.json'))
for path in files:
    value = json.loads(path.read_text(encoding='utf-8'))
    validator.validate(value)
command = json.loads((ROOT / 'compartido/ejemplos/04_lock_for.payload.json').read_text(encoding='utf-8'))
invalid = []
for field,value in [('durationSec',None),('durationSec',59),('durationSec',604801),('seq','0'),('seq','07'),('sender','CHILD'),('bootId','no-uuid'),('commandNonce','ABC'),('v',2)]:
    bad = copy.deepcopy(command); bad[field] = value; invalid.append(bad)
bad = copy.deepcopy(command); bad['unknownField'] = True; invalid.append(bad)
bad = copy.deepcopy(command); bad['action'] = 'LOCK'; invalid.append(bad)
state = json.loads((ROOT / 'compartido/ejemplos/10_state_inicial.payload.json').read_text(encoding='utf-8'))
bad = copy.deepcopy(state); bad['lastResult'] = 'APPLIED'; invalid.append(bad)
bad = copy.deepcopy(state); bad['lastAcceptedSeq'] = '7'; invalid.append(bad)
for index,value in enumerate(invalid,1):
    if validator.is_valid(value):
        raise AssertionError(f'Contraejemplo {index} aceptado indebidamente')
print(f'OK: esquema válido; {len(files)} ejemplos aceptados y {len(invalid)} contraejemplos rechazados.')
print('Es validación estructural: no comprueba claves, firmas, nonce fresco ni permisos reales.')
