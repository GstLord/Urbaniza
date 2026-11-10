# Guia de Execução

Passo a passo para instalar os pré-requisitos, preparar o banco de dados, compilar, executar e testar o Urbaniza em Windows, Linux e macOS.

Todos os comandos partem da **raiz do repositório** (pasta que contém o `README.md` principal e a pasta `Urbaniza/`), salvo indicação em contrário.

## 1. Pré-requisitos

| Software | Versão | Para que serve | Download oficial |
|----------|--------|----------------|------------------|
| JDK (Java Development Kit) | 21 | Compilar e executar a aplicação | [Eclipse Temurin 21 (Adoptium)](https://adoptium.net/temurin/releases/?version=21) ou [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21) |
| Apache Maven | 3.9 ou superior | Build, testes e geração do jar | [maven.apache.org/download.cgi](https://maven.apache.org/download.cgi) (instruções em [maven.apache.org/install.html](https://maven.apache.org/install.html)) |
| Docker Desktop (Windows/macOS) ou Docker Engine + Compose (Linux) | Atual | Subir o PostgreSQL 16 em contêiner | [Docker Desktop](https://www.docker.com/products/docker-desktop/) / [Docker Engine](https://docs.docker.com/engine/install/) |
| PostgreSQL (somente sem Docker) | 16 | Banco de dados local | [postgresql.org/download](https://www.postgresql.org/download/) |
| Git | Atual | Clonar o repositório | [git-scm.com/downloads](https://git-scm.com/downloads) |

O Docker e o PostgreSQL local são alternativos: basta um dos dois.

### 1.1 Windows

1. Instale o JDK 21 pelo instalador `.msi` do Temurin, marcando a opção que define a variável `JAVA_HOME` e adiciona o Java ao `PATH`.
2. Baixe o arquivo binário do Maven (`apache-maven-3.9.x-bin.zip`), extraia-o (por exemplo, em `C:\Program Files\Apache\maven`) e adicione a subpasta `bin` à variável de ambiente `Path` (Configurações > Sistema > Sobre > Configurações avançadas do sistema > Variáveis de Ambiente).
3. Instale o Docker Desktop. Ele utiliza o WSL 2; siga as instruções do instalador e reinicie o computador se solicitado.
4. Abra um **novo** terminal PowerShell para que as variáveis de ambiente sejam recarregadas.

### 1.2 Linux

1. Instale o JDK 21 pelo gerenciador de pacotes da distribuição ou pelo Temurin (por exemplo, no Ubuntu/Debian, o pacote `openjdk-21-jdk`).
2. Instale o Maven 3.9 ou superior. Se a versão disponível no gerenciador de pacotes for anterior à 3.9, use o binário oficial: extraia o arquivo e adicione a pasta `bin` ao `PATH` no `~/.bashrc`.
3. Instale o Docker Engine e o plugin Compose conforme a documentação oficial da sua distribuição. Para usar o Docker sem `sudo`, adicione seu usuário ao grupo `docker` e inicie uma nova sessão.

### 1.3 macOS

1. Instale o JDK 21 pelo instalador `.pkg` do Temurin (escolha a versão para Apple Silicon/aarch64 ou Intel/x64 conforme o processador).
2. Instale o Maven pelo binário oficial (adicionando a pasta `bin` ao `PATH` no `~/.zshrc`) ou pelo Homebrew, se já o utiliza.
3. Instale o Docker Desktop para macOS (versão para Apple Silicon ou Intel).

## 2. Verificar as versões instaladas

Os comandos são os mesmos em PowerShell, bash e zsh:

```
java -version
mvn -version
docker --version
docker compose version
```

Resultado esperado: `java -version` informa a versão 21; `mvn -version` informa Apache Maven 3.9 ou superior e "Java version: 21"; o Docker responde aos dois últimos comandos. Se `mvn -version` mostrar outra versão do Java, ajuste `JAVA_HOME` (ver seção 9).

## 3. Obter o código

```
git clone https://github.com/GstLord/Urbaniza.git
cd Urbaniza
```

A pasta clonada é a raiz do repositório; o projeto de código fica na subpasta `Urbaniza/`.

## 4. Subir o banco de dados com Docker

Certifique-se de que o Docker Desktop (ou o serviço do Docker Engine) está em execução.

Linux, macOS e PowerShell 7:

```
cd Urbaniza/database && docker compose up -d
```

Windows PowerShell 5.1 (não aceita `&&`):

```powershell
cd Urbaniza/database; docker compose up -d
```

O arquivo `docker-compose.yml` cria o contêiner `urbaniza-db` (imagem `postgres:16`), com banco `urbaniza`, usuário `urbaniza`, senha `urbaniza` e porta `5432`. Na primeira criação do volume `urbaniza_pgdata`, os scripts `01_schema.sql` (tabelas) e `02_dados_exemplo.sql` (dados de exemplo) são executados automaticamente.

Para conferir se o banco está pronto:

```
docker ps
docker logs urbaniza-db
```

O contêiner `urbaniza-db` deve aparecer em execução, e os logs devem indicar que o banco está pronto para aceitar conexões ("database system is ready to accept connections"), sem mensagens de `ERROR`.

Volte à raiz do repositório antes do próximo passo (`cd ../..`).

## 5. Alternativa sem Docker: PostgreSQL 16 local

### 5.1 Criar usuário e banco

Conecte-se como superusuário do PostgreSQL (normalmente `postgres`) e execute:

```sql
CREATE USER urbaniza WITH PASSWORD 'urbaniza';
CREATE DATABASE urbaniza OWNER urbaniza;
```

Formas de abrir o `psql` como superusuário:

- Windows (PowerShell): `psql -U postgres` (o executável fica em `C:\Program Files\PostgreSQL\16\bin`; adicione essa pasta ao `Path` se o comando não for encontrado).
- Linux: `sudo -u postgres psql`
- macOS: `psql -U postgres` (ou `psql postgres`, conforme a forma de instalação).

### 5.2 Executar os scripts

A partir da raiz do repositório:

```
psql -U urbaniza -d urbaniza -f Urbaniza/database/01_schema.sql
psql -U urbaniza -d urbaniza -f Urbaniza/database/02_dados_exemplo.sql
```

Observações:

- No Linux, se a autenticação local for do tipo `peer`, acrescente `-h localhost` para conectar por TCP com senha: `psql -h localhost -U urbaniza -d urbaniza -f Urbaniza/database/01_schema.sql`.
- Para não digitar a senha a cada comando, defina `PGPASSWORD` antes de executar os scripts: `$env:PGPASSWORD="urbaniza"` (PowerShell) ou `export PGPASSWORD=urbaniza` (bash/zsh).

## 6. Compilar

A partir da raiz do repositório.

Linux, macOS e PowerShell 7:

```
cd Urbaniza && mvn clean package
```

Windows PowerShell 5.1:

```powershell
cd Urbaniza; mvn clean package
```

O Maven baixa as dependências na primeira execução, compila o código, executa os testes unitários e gera `Urbaniza/target/urbaniza.jar`. A mensagem final esperada é `BUILD SUCCESS`.

Volte à raiz do repositório antes do próximo passo (`cd ..`).

## 7. Executar

A partir da raiz do repositório (igual nos três sistemas):

```
java -jar Urbaniza/target/urbaniza.jar
```

A aplicação testa a conexão com o banco e abre o menu principal:

```
1 Regiões
2 Loteamentos
3 Equipamentos comunitários
4 Analistas
5 Critérios de vedação
6 Análises de parcelamento
0 Sair
```

Se a conexão falhar, a aplicação exibe uma mensagem explicativa e encerra; confira a seção 9.

### 7.1 Configuração da conexão

Por padrão, a aplicação usa os valores do arquivo `Urbaniza/src/main/resources/db.properties` (chaves `db.url`, `db.user` e `db.password`):

| Parâmetro | Valor padrão |
|-----------|--------------|
| URL | `jdbc:postgresql://localhost:5432/urbaniza` |
| Usuário | `urbaniza` |
| Senha | `urbaniza` |

As variáveis de ambiente abaixo têm prioridade sobre o arquivo e permitem apontar para outro banco sem recompilar.

Windows (PowerShell), válidas para a sessão atual do terminal:

```powershell
$env:URBANIZA_DB_URL="jdbc:postgresql://localhost:5432/urbaniza"
$env:URBANIZA_DB_USER="urbaniza"
$env:URBANIZA_DB_PASSWORD="urbaniza"
java -jar Urbaniza/target/urbaniza.jar
```

Linux e macOS (bash/zsh):

```bash
export URBANIZA_DB_URL=jdbc:postgresql://localhost:5432/urbaniza
export URBANIZA_DB_USER=urbaniza
export URBANIZA_DB_PASSWORD=urbaniza
java -jar Urbaniza/target/urbaniza.jar
```

## 8. Testes

### 8.1 Testes unitários

Não precisam do banco. A partir da pasta `Urbaniza`:

```
mvn test
```

### 8.2 Testes de integração

Exigem o banco no ar com os dados de exemplo. São ativados pela variável de ambiente `URBANIZA_IT=true`. A partir da pasta `Urbaniza`:

Windows (PowerShell):

```powershell
$env:URBANIZA_IT="true"; mvn test
```

Para desativá-los novamente na mesma sessão: `Remove-Item Env:URBANIZA_IT`.

Linux e macOS (bash/zsh):

```bash
URBANIZA_IT=true mvn test
```

### 8.3 Recriar o banco do zero

Apaga o volume (todos os dados, inclusive análises emitidas) e reexecuta os scripts de criação e de dados de exemplo.

Linux, macOS e PowerShell 7:

```
cd Urbaniza/database && docker compose down -v && docker compose up -d
```

Windows PowerShell 5.1:

```powershell
cd Urbaniza/database; docker compose down -v; docker compose up -d
```

Sem Docker, recrie o banco pelo `psql` como superusuário e execute novamente os dois scripts (seção 5):

```sql
DROP DATABASE urbaniza;
CREATE DATABASE urbaniza OWNER urbaniza;
```

## 9. Solução de problemas

| Sintoma | Causa provável | Solução |
|---------|----------------|---------|
| `docker compose up -d` falha com "port is already allocated" ou "address already in use" | A porta 5432 já está em uso, geralmente por um PostgreSQL instalado localmente. | Pare o serviço local (Windows: `services.msc`, serviço "postgresql-x64-16"; Linux: `sudo systemctl stop postgresql`; macOS: pelo aplicativo ou `brew services stop`), ou altere a porta publicada em `Urbaniza/database/docker-compose.yml` (por exemplo, `"5433:5432"`) e defina `URBANIZA_DB_URL=jdbc:postgresql://localhost:5433/urbaniza`. |
| "Cannot connect to the Docker daemon" ou "error during connect" | O Docker não está em execução. | Abra o Docker Desktop e aguarde até que ele indique que está em execução; no Linux, `sudo systemctl start docker`. |
| `mvn` não é reconhecido | A pasta `bin` do Maven não está no `PATH`. | Ajuste o `PATH` (seção 1) e abra um novo terminal. |
| `mvn -version` mostra Java diferente de 21, ou o build falha com "release version 21 not supported" | `JAVA_HOME` aponta para outro JDK. | Windows (PowerShell, sessão atual): `$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21..."` (ajuste ao caminho instalado) ou defina-a permanentemente em Variáveis de Ambiente. Linux/macOS: `export JAVA_HOME=<pasta do JDK 21>` (no macOS, `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`). |
| A aplicação informa falha de conexão | Banco parado, porta diferente ou credenciais incorretas. | Confira `docker ps`, os valores de `URBANIZA_DB_URL`, `URBANIZA_DB_USER` e `URBANIZA_DB_PASSWORD` e, sem Docker, se o serviço do PostgreSQL está ativo. |
| As tabelas não existem ou os dados de exemplo não aparecem | O volume já existia quando os scripts foram alterados; os scripts de inicialização só rodam na primeira criação do volume. | Recrie o banco do zero (seção 8.3). |
| Acentos aparecem incorretos no terminal do Windows | Página de código do console diferente de UTF-8. | Execute `chcp 65001` antes de `java -jar`, ou use o Windows Terminal. |
| `psql` falha com "Peer authentication failed" (Linux) | Autenticação local do tipo `peer`. | Acrescente `-h localhost` ao comando `psql`. |
