```yaml
cat <<EOF > /tmp/demo-rabbitmq.yaml
apiVersion: rabbitmq.com/v1beta1
kind: RabbitmqCluster
metadata:
  name: demo-rabbitmq
spec:
  imagePullSecrets:
  - name: tap-registry
EOF
```

```
kubectl apply -f /tmp/demo-rabbitmq.yaml -n ${NAMESPACE}
```

```
tanzu apps workload apply demo-rabbitmq-sender \
  --app demo-rabbitmq-sender \
  --git-repo https://github.com/making/demo-rabbitmq-sender \
  --git-branch main \
  --type web \
  --build-env BP_JVM_VERSION=17 \
  --annotation autoscaling.knative.dev/minScale=1 \
  --service-ref demo-rabbitmq=rabbitmq.com/v1beta1:RabbitmqCluster:demo-rabbitmq \
  -n ${NAMESPACE}
```

```yaml
cat <<EOF > /tmp/demo-db.yaml
apiVersion: sql.tanzu.vmware.com/v1
kind: Postgres
metadata:
  name: demo-db
spec:
  storageClassName: k8s-storage
  storageSize: 1Gi
  cpu: "0.25"
  memory: 256Mi
  imagePullSecret:
    name: tap-registry
  monitorStorageClassName: k8s-storage
  monitorStorageSize: 1Gi
  resources:
    monitor:
      limits:
        cpu: 256m
        memory: 256Mi
      requests:
        cpu: 128m
        memory: 128Mi
  pgConfig:
    username: pgadmin
    appUser: demo
    dbname: demo
  postgresVersion:
    name: postgres-14  
  serviceType: ClusterIP
  highAvailability:
    enabled: false
EOF
```

```
kubectl apply -f /tmp/demo-db.yaml -n ${NAMESPACE}
```

```
curl https://demo-rabbitmq-sender-${NAMESPACE}.apps.jaguchi.maki.lol/send -d count=10000
```

```
kubectl port-forward -n ${NAMESPACE} svc/demo-rabbitmq 15672:15672
```

```yaml
cat <<EOF > /tmp/cronjob.yaml
apiVersion: servicebinding.io/v1beta1
kind: ServiceBinding
metadata:
  name: demo-rabbitmq-reader-demo-rabbitmq
spec:
  name: demo-rabbitmq
  service:
    apiVersion: rabbitmq.com/v1beta1
    kind: RabbitmqCluster
    name: demo-rabbitmq
  workload:
    apiVersion: batch/v1
    kind: Job
    selector:
      matchLabels:
        cron-job-name: demo-rabbitmq-reader
---
apiVersion: servicebinding.io/v1beta1
kind: ServiceBinding
metadata:
  name: demo-rabbitmq-reader-demo-db
spec:
  name: demo-db
  service:
    apiVersion: sql.tanzu.vmware.com/v1
    kind: Postgres
    name: demo-db
  workload:
    apiVersion: batch/v1
    kind: Job
    selector:
      matchLabels:
        cron-job-name: demo-rabbitmq-reader
---
apiVersion: batch/v1
kind: CronJob
metadata:
  name: demo-rabbitmq-reader
spec:
  jobTemplate:
    metadata:
      name: demo-rabbitmq-reader
      labels:
        cron-job-name: demo-rabbitmq-reader
    spec:
      template:
        spec:
          containers:
          - image: ghcr.io/making/demo-rabbitmq-reader
            name: demo-rabbitmq-reader
            env:
            - name: SPRING_PROFILES_ACTIVE
              value: postgresql
          restartPolicy: OnFailure
  schedule: '*/30 * * * *'
EOF
```

```
kubectl create job -n ${NAMESPACE} --from cronjob/demo-rabbitmq-reader demo-rabbitmq-reader-${RANDOM}
kubectl create job -n ${NAMESPACE} --from cronjob/demo-rabbitmq-reader demo-rabbitmq-reader-${RANDOM}
kubectl create job -n ${NAMESPACE} --from cronjob/demo-rabbitmq-reader demo-rabbitmq-reader-${RANDOM}
```
