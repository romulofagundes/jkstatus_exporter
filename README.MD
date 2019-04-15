## JK Status Exporter

Um projeto simples em que faz o parse do arquivo gerado pela página do JKStatus, em formato XML para o formato do Prometheus.

Não foi utilizado a lib do prometheus client.

O projeto possui um Dockerfile simples, e uma imagem no Dockerhub, para fazer uso, utilize o seguinte formato de arquivo _config.yml_

```yaml
remote:
  url: https://remote/
  context: jkstatus
  login: admin
  pass: secret

local:
  url: http://127.0.0.1/
  context: jkstatus
```

Para adicionar em um arquivo _docker-compose.yml_ utilize o seguinte formato de exemplo:

```yaml
jkstatus-exporter:
    image: romulofc/jkstatus_exporter
    container_name: jkstatus-exporter
    volumes:
    - ./config.yml:/etc/jkstatus_exporter/config.yml:ro
    ports: 
    - 9573:9573
```

Para realizar alteração e geração de um novo JAR, execute a seguinte sequência:

```bash
gradle clean
gradle assemble
```

Caso deseje executar de forma isolada e em formato de serviço, execute:

```bash
docker run -it --name jkstatus_exporter -v config.yml:/etc/jkstatus_exporter/config.yml:ro -p 9573:9573 romulofc/jkstatus_exporter
```

Para que a chamada no Prometheus funcione de forma correta, adicione o seguinte trecho:
```yaml
- job_name: 'jkstatus-exporter'
    metrics_path: /probe
    static_configs:
      - targets:
        - remote
        - local
    relabel_configs:
      - source_labels: [__address__]
        target_label: __param_target
      - source_labels: [__param_target]
        target_label: instance
      - target_label: __address__
        replacement: jkstatus-exporter:9571
```

Para testar se a comunicação ocorreu de forma correta, utilize a seguinte URL: http://127.0.0.1:9573/probe?target=remote .