echo "😶‍🌫️ Scale-to-0 infrastruttura gamems kub.."
K8S_DIR="./k8s/phase2_local"
kubectl scale deployment --all --replicas=0 -n gamems 
kubectl scale deployment mailpit-deployment --replicas=0 -n gamems

kubectl get pods -n gamems

echo "😉 Tutto spento FRATM"