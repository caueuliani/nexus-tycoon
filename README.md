# Nexus Tycoon

Jogo Android de gestão industrial espacial em Kotlin e Jetpack Compose, com banco local Room. Inclui produção automática, mercado, combate, cientistas, missões e prestígio.

**Estado: desenvolvimento. Não aprovado para produção.** Login, monetização e recursos sociais ainda contêm demonstrações; a presença das telas não significa integração com serviços reais.

## Compilar e testar

Requisitos: JDK 17 ou 21 para compilação, JDK 21 para os testes Robolectric de Android 16, Android SDK Platform 36.1 e Build Tools 36.0.0. O Gradle Wrapper fixa a versão 9.3.1 e verifica seu checksum.

Abra o projeto no Android Studio ou configure `ANDROID_HOME`/`sdk.dir` em `local.properties`. No Windows:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --continue
```

Em macOS/Linux, use `./gradlew`. O debug utiliza a assinatura padrão de desenvolvimento. O APK local fica em `app/build/outputs/apk/debug/`; o APK antigo de `.build-outputs/` não comprova o estado da revisão atual.

`BackupProductionAuditTest` verifica preservação de progresso após restauração em uma instalação nova. `LaunchProductionAuditTest` verifica a abertura da Activity no Robolectric, sem substituir execução em aparelho real.

O backup usa o formato 2, incluindo cientistas, missões e níveis de pesquisa. Backups antigos incompletos são recusados antes de alterar dados locais; gere um novo backup no aparelho de origem. O modo de demonstração guarda arquivos locais duráveis separados por conta e não substitui a nuvem real. A restauração é transacional; a concorrência geral entre ações do jogo e o laço de produção ainda precisa de revisão.

## Publicação

A variante release exige `KEYSTORE_PATH`, `STORE_PASSWORD` e `KEY_PASSWORD`; o alias configurado é `upload`. Mantenha a chave privada e senhas fora do repositório. Gere o bundle com `:app:bundleRelease` somente depois das validações funcionais.

Antes de publicar, implementar e validar login real, regras Firebase, backup completo, migrações sem perda, Google Play Billing, anúncios, persistência de eventos, testes em aparelhos e configurações da Play Console.

Origem: [Google AI Studio](https://ai.studio/apps/9f83ca06-d8b1-4014-ad8f-51713b1acf31).

## Configuração externa recebida

Firebase: `nexus-idle-tycoon`. Cadastre o aplicativo Android com o pacote
`com.aistudio.idletycoon.nxtyc`, habilite Google Authentication, registre os certificados
SHA da assinatura usada e coloque a configuração baixada em `app/google-services.json`.
O arquivo é ignorado pelo Git. O login permanece indisponível sem essa configuração.
O fluxo usa Credential Manager e troca o token Google por uma sessão Firebase.
Backups reais usam `users/{uid}`; não usam e-mail como chave de autorização.
As regras do servidor precisam ser revisadas e testadas antes da ativação.

## Anúncios recompensados

App ID de produção: `ca-app-pub-1910491454764587~5605387336`.
Bloco de produção: `ca-app-pub-1910491454764587/7433492171`.
O debug usa App ID e bloco oficiais de demonstração. A loja possui um botão opcional
para ganhar 5 gemas após a confirmação do SDK, com UMP e seleção de faixa etária.
Sem consentimento elegível não são solicitados anúncios. A migração Room 8 → 9 adiciona
recibos locais para impedir que um mesmo callback conceda gemas duas vezes.

Testes locais verificam migração, persistência e idempotência; não comprovam a exibição
em aparelho. Mensagens de privacidade no AdMob, teste completo de ciclo de vida,
verificação de recompensas no servidor e compras Google Play continuam pendentes.
O parâmetro Gradle `ADMOB_REWARDED_AD_UNIT_ID` permite sobrescrever o bloco de release.
Nenhuma chave de assinatura ou configuração Firebase é distribuída no ZIP de fontes.
Não publicar como produção antes de concluir essas validações.
