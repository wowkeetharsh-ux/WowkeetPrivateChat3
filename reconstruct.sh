#!/usr/bin/env bash
set -e
mkdir -p app functions
python3 - <<'PY'
import os, shutil
for f in os.listdir("."):
    if "__" not in f or f in ("reconstruct.sh",): continue
    if f.startswith(".github__"): continue
    rel=f.replace("__","/")
    dest=os.path.join(".", rel)
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    shutil.copyfile(f,dest)
PY
