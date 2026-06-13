echo "😶‍🌫️ Scale-to-0 infrastruttura gamems kub..."
kubectl scale deployment --all --replicas=0 -n gamems 
kubectl scale deployment mailpit-deployment --replicas=0 -n gamems

kubectl get pods -n gamems

echo "😉 Tutto spento FRATM"