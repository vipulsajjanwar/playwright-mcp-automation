# Elasticsearch & Kibana on WSL: Setup, Clustering, Data Migration and Java Integration

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start: Single-Node Setup with Docker](#quick-start-single-node-setup-with-docker)
4. [Port Mapping for Windows Host Access](#port-mapping-for-windows-host-access)
5. [Static IP for WSL](#static-ip-for-wsl)
6. [Accessing Elasticsearch and Kibana](#accessing-elasticsearch-and-kibana)
7. [Exporting and Importing Data with Elasticdump](#exporting-and-importing-data-with-elasticdump)
8. [Docker Utilities](#docker-utilities)
9. [Elasticsearch REST API Cheat Sheet](#elasticsearch-rest-api-cheat-sheet)
10. [Secure Multi-Node Cluster (7.17.26) with TLS](#secure-multi-node-cluster-71726-with-tls)
11. [Java Integration: `ELKUtils`](#java-integration-elkutils)
12. [WSL Monitoring and Utility Tools](#wsl-monitoring-and-utility-tools)
13. [Conclusion](#conclusion)

---

## Overview

Elasticsearch and Kibana are core components of the Elastic Stack.

- **Elasticsearch**: a distributed, open-source search and analytics engine for large volumes of structured and unstructured data.
- **Kibana**: the UI for visualizing data stored in Elasticsearch and building dashboards.

This guide covers installation on Windows Subsystem for Linux (WSL), port forwarding for access from Windows, data export/import, a secure two-node TLS cluster, a Java utility that pushes test results to Elasticsearch, and handy WSL monitoring tools.

---

## Prerequisites

| Requirement | Notes |
|---|---|
| WSL 2 | Recommended for performance |
| Docker and Docker Compose | Run the containers inside WSL |
| Basic Linux command-line skills | Needed to follow the commands |

### Check or install WSL

```powershell
# Check the current version
wsl --version

# If WSL is not installed
wsl --install
wsl --set-default-version 2
wsl --install -d Ubuntu-20.04
wsl --shutdown
```

### Install Docker and Docker Compose (inside WSL)

```bash
sudo apt update
sudo apt install docker.io docker-compose
```

---

## Quick Start: Single-Node Setup with Docker

Create a `docker-compose.yml` in your working directory that defines the Elasticsearch and Kibana services (see the [multi-node example](#docker-composeyml) below for a full reference), then run:

```bash
# Start services in the background
docker-compose up -d

# Stop and remove services
docker-compose down
```

> `docker-compose up -d` must be run from the directory that contains `docker-compose.yml`.

---

## Port Mapping for Windows Host Access

To reach Kibana and Elasticsearch from Windows, forward the ports with `netsh`.

Open **Command Prompt or PowerShell as Administrator** and run:

```powershell
netsh interface portproxy add v4tov4 listenport=5601 listenaddress=0.0.0.0 connectport=5601 connectaddress=<wsl-ip> protocol=tcp
netsh interface portproxy add v4tov4 listenport=9200 listenaddress=0.0.0.0 connectport=9200 connectaddress=<wsl-ip> protocol=tcp
```

Get `<wsl-ip>` by running this in your WSL terminal:

```bash
ip a
```

Verify the rules:

```powershell
netsh interface portproxy show all
```

---

## Static IP for WSL

For a consistent address across restarts, assign a static IP in WSL:

```bash
sudo ip addr add <Host-IP>/24 dev eth0
```

Replace `<Host-IP>` with the address you want to assign.

> This setting is not persistent. It is lost when WSL restarts, so re-apply it (or script it) after each restart.

---

## Accessing Elasticsearch and Kibana

| Service | URL |
|---|---|
| Elasticsearch | `http://<Host-IP>:9200` |
| Kibana | `http://<Host-IP>:5601` |

Replace `<Host-IP>` with your WSL IP or the static IP assigned above.

---

## Exporting and Importing Data with Elasticdump

Elasticdump bulk-exports data from one Elasticsearch instance and imports it into another.

### 1. Install Node.js and npm

```bash
sudo apt install nodejs npm -y
node -v
npm -v
```

### 2. Install Elasticdump

```bash
npm install -g elasticdump
elasticdump --version
```

### 3. Export data

```bash
elasticdump \
  --input=http://elastic:your_password@localhost:9200 \
  --output=./all-data.json \
  --type=data
```

Replace `your_password` with your Elasticsearch password. Data is saved to `all-data.json`.

> `--type=data` exports documents only. To migrate index structure as well, run separate exports with `--type=mapping` (and `--type=settings`) before the data import.

### 4. Install Elasticdump on the target machine

```bash
npm install -g elasticdump
```

### 5. Import data into the target instance

```bash
elasticdump \
  --input=./all-data.json \
  --output=http://target_es_host:9200 \
  --type=data
```

Replace `target_es_host` with the address of the target Elasticsearch instance.

---

## Docker Utilities

### Open a root shell in the Kibana container

```bash
docker exec -it --user root kibana bash
```

### Copy Kibana data to the local machine (backup)

```bash
docker cp kibana:/usr/share/kibana/data ./kibana_data_backup
```

---

## Elasticsearch REST API Cheat Sheet

### 1. Search an index with a query payload

```http
GET /<index>/_search
{
  "query": {
    "term": {
      "module": "ModuleName"
    }
  }
}
```

### 2. Update documents by query

```http
POST /<index>/_update_by_query
{
  "script": {
    "source": "if (ctx._source['module'] == 'ModuleName') { ctx._source['module'] = 'NewModuleName' }",
    "lang": "painless"
  },
  "query": {
    "match": {
      "module": "ModuleName"
    }
  }
}
```

### 3. Get index mapping

```http
GET /<index>/_mapping
```

### 4. Document CRUD

| Operation | Request |
|---|---|
| Create (auto ID) | `POST /<index>/<type>` |
| Create (with ID) | `POST /<index>/<type>/<id>` |
| Create or replace | `PUT /<index>/<type>/<id>` |
| Read | `GET /<index>/<type>/<id>` |
| Delete | `DELETE /<index>/<type>/<id>` |

> Mapping types are deprecated in 7.x and removed in 8.x. On 7.17, prefer `/<index>/_doc/<id>`.

---

## Secure Multi-Node Cluster (7.17.26) with TLS

A two-node Elasticsearch cluster with Kibana, using Docker and TLS certificates for node-to-node (transport) encryption.

### 1. Directory structure (host machine)

```text
ELK-setup/
├── certs/
│   ├── ca/
│   │   ├── ca.crt
│   │   └── ca.key
│   ├── elasticsearch1/
│   │   ├── elasticsearch1.crt
│   │   └── elasticsearch1.key
│   ├── elasticsearch2/
│   │   ├── elasticsearch2.crt
│   │   └── elasticsearch2.key
│   └── instances.yml
└── docker-compose.yml
```

### 2. `instances.yml` (for certificate generation)

Save this as `certs/instances.yml`:

```yaml
instances:
  - name: elasticsearch1
    dns:
      - elasticsearch1
  - name: elasticsearch2
    dns:
      - elasticsearch2
```

### 3. Generate the CA and certificates

**Generate the CA:**

```bash
docker run --rm -v $PWD/certs:/certs \
  docker.elastic.co/elasticsearch/elasticsearch:7.17.26 \
  bin/elasticsearch-certutil ca --pem --out /certs/ca.zip
```

**Unzip the CA:**

```bash
unzip certs/ca.zip -d certs/ca
```

**Generate instance certificates:**

```bash
docker run --rm -v $PWD/certs:/certs \
  docker.elastic.co/elasticsearch/elasticsearch:7.17.26 \
  bin/elasticsearch-certutil cert --pem --in /certs/instances.yml \
  --ca-cert /certs/ca/ca.crt --ca-key /certs/ca/ca.key --out /certs/certs.zip
```

**Unzip the certificates:**

```bash
unzip certs/certs.zip -d certs
```

### 4. `docker-compose.yml`

```yaml
version: '3.8'

services:
  elasticsearch1:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.17.26
    container_name: elasticsearch1
    environment:
      - node.name=elasticsearch1
      - cluster.name=es-cluster
      - discovery.seed_hosts=elasticsearch2
      - cluster.initial_master_nodes=elasticsearch1,elasticsearch2
      - xpack.security.enabled=true
      - xpack.security.transport.ssl.enabled=true
      - xpack.security.transport.ssl.verification_mode=certificate
      - xpack.security.transport.ssl.key=/usr/share/elasticsearch/config/certs/elasticsearch1/elasticsearch1.key
      - xpack.security.transport.ssl.certificate=/usr/share/elasticsearch/config/certs/elasticsearch1/elasticsearch1.crt
      - xpack.security.transport.ssl.certificate_authorities=/usr/share/elasticsearch/config/certs/ca/ca.crt
      - ELASTIC_PASSWORD=elastic
    volumes:
      - ./certs:/usr/share/elasticsearch/config/certs
      - ./elasticsearchdata1:/usr/share/elasticsearch/data
    ports:
      - 9200:9200
      - 9300:9300
    networks:
      - elastic

  elasticsearch2:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.17.26
    container_name: elasticsearch2
    environment:
      - node.name=elasticsearch2
      - cluster.name=es-cluster
      - discovery.seed_hosts=elasticsearch1
      - cluster.initial_master_nodes=elasticsearch1,elasticsearch2
      - xpack.security.enabled=true
      - xpack.security.transport.ssl.enabled=true
      - xpack.security.transport.ssl.verification_mode=certificate
      - xpack.security.transport.ssl.key=/usr/share/elasticsearch/config/certs/elasticsearch2/elasticsearch2.key
      - xpack.security.transport.ssl.certificate=/usr/share/elasticsearch/config/certs/elasticsearch2/elasticsearch2.crt
      - xpack.security.transport.ssl.certificate_authorities=/usr/share/elasticsearch/config/certs/ca/ca.crt
      - ELASTIC_PASSWORD=elastic
    volumes:
      - ./certs:/usr/share/elasticsearch/config/certs
      - ./elasticsearchdata2:/usr/share/elasticsearch/data
    networks:
      - elastic

  kibana:
    image: docker.elastic.co/kibana/kibana:7.17.26
    container_name: kibana
    ports:
      - 5601:5601
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch1:9200
      - ELASTICSEARCH_USERNAME=elastic
      - ELASTICSEARCH_PASSWORD=elastic
      - SERVER_HOST=0.0.0.0
    depends_on:
      - elasticsearch1
      - elasticsearch2
    networks:
      - elastic

networks:
  elastic:
    driver: bridge
```

> **Security notes**
> - `ELASTIC_PASSWORD=elastic` is acceptable for a local sandbox only. Use a strong password (ideally via an `.env` file or Docker secrets) anywhere else.
> - TLS here covers **transport** (node-to-node) traffic only. The HTTP layer (port 9200) is still plain HTTP, so credentials travel unencrypted. Enable `xpack.security.http.ssl.*` if the cluster is reachable beyond localhost.

### 5. Run the setup

```bash
# Start the stack
docker-compose -f docker-compose.yml up -d

# Check cluster health
curl -u elastic:elastic "http://localhost:9200/_cluster/health?pretty"
```

### 6. Debugging

```bash
docker logs elasticsearch1
docker logs elasticsearch2
docker logs kibana
```

### 7. Cluster status is yellow?

A yellow status is expected on a 2-node cluster when replica shards cannot be allocated. Fix it by either:

- setting `number_of_replicas: 0` on the affected indices, or
- adding a third node for full high availability.

---

## Java Integration: `ELKUtils`

A utility class that posts test execution details (name, status, module, author, build, release, and so on) to Elasticsearch after each test.

```java
package nexgen.automation.framework.reports;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import nexgen.automation.framework.baseclass.BaseSuite;
import nexgen.automation.framework.config.ReadConfig;
import nexgen.automation.framework.constant.Constant;
import nexgen.automation.framework.constant.ConstantProperty;
import nexgen.automation.framework.util.CustomTimeUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ELKUtils extends BaseSuite {

    private ELKUtils() {}

    /** static boolean flag = Constant.ELK_STATUS; **/
    static boolean flag = BaseSuite.elkFlag;
    static String elkurl = Constant.ELK_URL.toLowerCase();
    static String elkusername = Constant.ELK_USERNAME;
    static String elkpassword = Constant.ELK_PASSWORD;
    static ReadConfig mailconfig = new ReadConfig();

    public static void sendDetailsToElk(String testname, String status, String author,
            String testCaseId, String testgroups, String icedqUser) {
        try {
            if (flag) {
                String xrayid = "";
                String credentials = elkusername + ":" + elkpassword;
                String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

                if (testCaseId.toLowerCase().contains("nrr")) {
                    xrayid = mailconfig.xRayBaseUrl() + testCaseId;
                } else {
                    xrayid = testCaseId;
                }

                author = author.isEmpty() ? Constant.AUTHOR_DHAWALT : author;

                OkHttpClient client = new OkHttpClient().newBuilder().build();
                MediaType mediaType = MediaType.parse("application/json");

                // Prepare the JSON body as a string
                String requestBodyContent = "{\r\n"
                        + "  \"testName\": \"" + testname + "\",\r\n"
                        + "  \"status\": \"" + status + "\",\r\n"
                        + "  \"module\": \"" + getModuleName(BaseSuite.ModuleName) + "\",\r\n"
                        + "  \"author\": \"" + author + "\",\r\n"
                        + "  \"executionTime\": \"" + CustomTimeUtil.getUTCTimeFormat("yyyy/MM/dd HH:mm:ss") + "\",\r\n"
                        + "  \"executionDate\": \"" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "\",\r\n"
                        + "  \"testCaseId\": \"" + xrayid + "\",\r\n"
                        + "  \"instanceURL\": \"" + trimLastCharIfSlash(BaseSuite.baseURL) + "\",\r\n"
                        + "  \"executionMessage\": \"" + BaseSuite.subjectMessage + "\",\r\n"
                        + "  \"buildId\": \"" + BaseSuite.buildId.toLowerCase() + "\",\r\n"
                        + "  \"releaseId\": \"" + BaseSuite.releaseName + "\",\r\n"
                        + "  \"icedqUser\": \"" + getUserFullName(icedqUser) + "\",\r\n"
                        + "  \"testGroups\": \"" + testgroups + "\"\r\n"
                        + "}";

                RequestBody body = RequestBody.create(mediaType, requestBodyContent);

                String requestUrl = elkurl + BaseSuite.suiteType.toLowerCase() + "-"
                        + BaseSuite.buildId.toLowerCase() + "/result/" + testCaseId.toLowerCase();

                log.info("ElasticSearch Request URL: " + requestUrl);
                BaseSuite.reportLog("ElasticSearch Request URL: " + requestUrl);
                log.info("ElasticSearch Request body: " + requestBodyContent);

                Request request = new Request.Builder()
                        .url(requestUrl)
                        .method("POST", body)
                        .addHeader("content-type", "application/json")
                        .addHeader("Authorization", "Basic " + encodedCredentials)
                        .build();

                Response response = client.newCall(request).execute();
                /** Assert.assertEquals(response.code(), 201); */
                log.info("ElasticSearch Response code: " + response.code());

                String responseBody = response.body().string(); // Get the response body as a string
                log.info("ElasticSearch Response Body: " + responseBody); // Log the response body
                /** BaseSuite.reportLog("ElasticSearch Response Body: " + responseBody); */
            }
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }

    public static String getModuleName(String moduleName) {
        String name = null;
        try {
            switch (moduleName.toLowerCase().trim()) {
                case "administration":
                    name = ConstantProperty.MODULE_ADMIN;
                    break;
                case "datatesting":
                    name = ConstantProperty.MODULE_DATATESTING;
                    break;
                case "connectionrepo":
                    name = ConstantProperty.MODULE_CONNECTIONS;
                    break;
                case "scheduler":
                    name = ConstantProperty.MODULE_Scheduler;
                    break;
                default:
                    name = moduleName;
                    break;
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return name;
    }

    public static String trimLastCharIfSlash(String input) {
        if (input != null && input.endsWith("/")) {
            return input.substring(0, input.length() - 1);
        }
        return input; // Return the original string if it doesn't end with '/'.
    }

    public static String getUserFullName(String icedqUser) {
        String user = "";
        switch (icedqUser.toLowerCase()) {
            case "account_owner":
                user = "Account Owner";
                break;
            case "account_contributor":
                user = "Account Contributor";
                break;
            case "account_executor":
                user = "Account Executor";
                break;
            case "workspace_owner":
                user = "Workspace Owner";
                break;
            case "workspace_contributor":
                user = "Workspace Contributor";
                break;
            case "workspace_executor":
                user = "Workspace Executor";
                break;
            case "application_admin":
                user = "Application Admin";
                break;
            default:
                user = "Account Owner";
                break;
        }
        return user;
    }
}
```

### Known weaknesses worth fixing

- **Hand-built JSON.** A quote, backslash, or newline in `testname`, `executionMessage`, etc. produces invalid JSON or a dropped document. Build the payload with Jackson or Gson instead of string concatenation.
- **Unclosed response.** `Response` should be closed (try-with-resources) to avoid leaking connections.
- **Credentials from constants.** Load the ELK username and password from environment variables or a secrets store, not source constants.
- **Silent failures.** Only `IOException` is caught and logged at `info`. A non-2xx response is logged but never flagged.
- **Deprecated `/result/` type.** The custom mapping type in the URL works on 7.17 but is deprecated and fails on 8.x. Use `/_doc`.

---

## WSL Monitoring and Utility Tools

### Built-in system commands

| Command | Description |
|---|---|
| `top` | Real-time process monitor |
| `free -h` | RAM usage (human-readable) |
| `df -h` | Disk usage of file systems |
| `du -sh *` | Disk usage of current folder contents |
| `ps aux` | List all running processes |
| `uptime` | System load and uptime |
| `who` / `w` | Logged-in users |
| `uname -a` | Kernel and system details |
| `lscpu` | CPU info |
| `lsblk` | Block devices (disks and partitions) |

### Recommended tools to install

#### htop: interactive system monitor

```bash
sudo apt update
sudo apt install htop
```

- A better `top`; shows CPU and RAM per process.
- Arrow keys to navigate, `F9` to kill, `F10` to quit.

#### ncdu: disk usage analyzer

```bash
sudo apt install ncdu
ncdu /
```

- Interactive view that makes large folders easy to find.

#### iotop: disk I/O monitor

```bash
sudo apt install iotop
sudo iotop
```

- Real-time read/write I/O per process (requires root).

#### iftop: network bandwidth monitor

```bash
sudo apt install iftop
sudo iftop
```

- Real-time bandwidth usage by IP and port.

#### netstat / ss: network connections

```bash
sudo apt install net-tools
netstat -tulnp    # list ports and owning processes

# or, without extra packages
ss -tulnp
```

#### glances: all-in-one dashboard

```bash
sudo apt install glances
glances
```

- CPU, memory, disk, processes, and network in one view.

#### bat: a better `cat`

```bash
sudo apt install bat
bat filename.txt
```

> On Ubuntu/Debian the binary may be installed as `batcat`.

### Misc tools for DevOps and testers

| Tool | Usage |
|---|---|
| `curl` / `wget` | API testing and downloads |
| `jq` | Pretty-print JSON: `cat file.json \| jq .` |
| `docker` | Container runtime (already installed if you followed this guide) |
| `git` | Version control |
| `tree` | Directory tree: `sudo apt install tree` |
| `watch` | Repeat a command periodically: `watch -n 1 df -h` |

### Handy one-liners

```bash
# Top 5 memory-consuming processes
ps aux --sort=-%mem | head -6

# CPU info
lscpu

# Current memory usage
free -h

# Which container is using the most resources
docker stats
```

### Install everything at once

```bash
sudo apt install htop ncdu iotop iftop glances tree jq curl wget git bat net-tools
```

---

## Conclusion

You now have a path from a basic Elasticsearch and Kibana install on WSL, through Windows port forwarding and Elasticdump migration, to a TLS-secured two-node cluster, a Java reporting hook, and a set of WSL monitoring tools. Before using any of this beyond a local sandbox, replace the default passwords, enable HTTP-layer TLS, and fix the issues listed in the Java section.