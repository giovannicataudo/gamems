import requests
import json
import re
import urllib.parse
import subprocess
import time

API_KEY = "LaTuaApiKeyEsternaSegreta2026!"
BASE_URL = "https://gamems.local/api/v1"
MAILPIT_URL = "http://localhost:32025/api/v1/messages"

email = "end2end2@example.com"
password = "password123"

print("1. Registrazione...")
res = requests.post(
    f"{BASE_URL}/auth/register",
    json={"email": email, "password": password},
    headers={"X-Api-Key": API_KEY, "Content-Type": "application/json"},
    verify=False
)
print("Risposta Registrazione:", res.status_code)

print("2. Recupero token da Mailpit...")
time.sleep(2)
res = requests.get(MAILPIT_URL).json()
latest_msg_id = res['messages'][0]['ID']
msg = requests.get(f"http://localhost:32025/api/v1/message/{latest_msg_id}").json()
token_match = re.search(r'token=([a-zA-Z0-9\-]+)', msg['Text'])
token = token_match.group(1)
print("Token trovato:", token)

print("3. Verifica Email e Recupero QR/Secret...")
res = requests.get(
    f"{BASE_URL}/auth/verify-email?token={token}",
    headers={"X-Api-Key": API_KEY},
    verify=False
).json()
print("Risposta Verifica Email:", res.get('message', 'Errore'))

# Extract secret from DB
print("4. Estrazione Secret MFA dal Database K8s...")
cmd = "kubectl exec $(kubectl get pods -n gamems -l app=postgres -o name) -n gamems -- psql -U user_user -d user_db -t -c \"SELECT mfa_secret FROM users WHERE email='end2end2@example.com';\""
secret = subprocess.check_output(cmd, shell=True).decode().strip()
print("Secret recuperato dal DB:", secret)

print("5. Generazione Codice TOTP...")
# Use oathtool
cmd = f"oathtool --totp -b {secret}"
otp = subprocess.check_output(cmd, shell=True).decode().strip()
print("Codice OTP generato:", otp)

print("6. Login (Step 1) - Richiesta credenziali...")
res = requests.post(
    f"{BASE_URL}/auth/login",
    json={"email": email, "password": password},
    headers={"X-Api-Key": API_KEY, "Content-Type": "application/json"},
    verify=False
)
print("Status:", res.status_code)
login_res = res.json()
print("Risposta Login (Step 1):", login_res)

temp_token = login_res.get('tempToken')

print("7. Login MFA (Step 2)...")
res = requests.post(
    f"{BASE_URL}/auth/login/mfa",
    json={"tempToken": temp_token, "code": otp},
    headers={"X-Api-Key": API_KEY, "Content-Type": "application/json"},
    verify=False
)
print("Status:", res.status_code)
final_res = res.json()
print("Token JWT Finale ricevuto:", final_res.get('token')[:30] + "...")

