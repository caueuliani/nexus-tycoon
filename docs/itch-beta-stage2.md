# Etapa 2 — itchBeta

Variante Android dedicada à beta itch, sem alterar o applicationId
`com.aistudio.idletycoon.nxtyc`. VersionName `1.0-itch-beta.1`; versionCode 1.
Não é uma migração do banco de dados nem uma nova etapa de gameplay.

## Alterações preservadas e concluídas

O diff recebido já continha o build type derivado de release, ITCH_BETA,
IDs comerciais vazios, ocultação dos cards comerciais e alterações de textos
na MainActivity, manifesto específico e PublicBetaConfigTest inicial.
Esta retomada preservou esse trabalho e adicionou bloqueio nos getters dos
controladores antes de construí-los, debuggable=false explícito no manifesto,
remoção de consultas/metadados Billing e testes adicionais de permissões,
atividades comerciais, controladores e trocas com moedas locais.

A loja conserva créditos por gemas, licença por 300 gemas e demais gastos
com moedas do jogo. Os SDKs continuam como dependências compartilhadas;
a variante não os remove do binário. Os caminhos comerciais da interface
estão ocultos e o acesso aos controladores pela Activity é bloqueado antes
da inicialização. Firebase/login/backup continuam conforme a implementação
existente; isto não é uma variante sem internet.

## Assinatura e instalação

itchBeta herda signingConfig release: alias `upload`, caminho por KEYSTORE_PATH
(ou my-upload-key.jks), senhas por STORE_PASSWORD e KEY_PASSWORD.
Nesta sessão as três variáveis não estavam configuradas e o keystore padrão
não existia. Não foi criada assinatura debug como substituição.
Nunca registrar senhas, conteúdo de keystore ou tokens no Git/log.

Como o applicationId é o mesmo, não instalar sobre o debug para validar esta
etapa. Nenhum comando de instalação/desinstalação ou publicação é necessário.
Uma entrega assinada exige disponibilidade da chave autorizada, verificação
do certificado e compatibilidade com a assinatura de atualizações anteriores.

## Validação desta retomada

Em 20/10/2026, compileItchBetaKotlin e processItchBetaMainManifest passaram.
Os passos de Java, recursos e compilação Kotlin dos testes também passaram.
O manifesto principal mesclado foi inspecionado: applicationId original,
versionName 1.0-itch-beta.1, debuggable=false, sem metadados AdMob/Billing,
sem AdActivity ou ProxyBillingActivity/V2 e sem consultas Billing/Play Store.
Permissões finais: INTERNET, ACCESS_NETWORK_STATE, READ_GSERVICES e a permissão
interna signature DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION. AD_ID, BILLING,
READ_BASIC_PHONE_STATE, FOREGROUND_SERVICE e WAKE_LOCK estão ausentes.
O BuildConfig gerado confirmou os IDs comerciais vazios e ITCH_BETA=true.

Foi necessário habilitar o HostTestBuilder de tipo UNIT_TEST_TYPE para o
build type customizado no AGP 9.1.1. Testes executam em JDK 21; compilação
usou JDK 17. Avisos de depreciação existentes e avisos de remoção de entradas
já ausentes no manifesto de testes não foram tratados como falhas.

`testItchBetaUnitTest` passou: 14 testes, zero falhas/erros/ignorados:
PublicBetaConfigTest (4), SaveSafetyTest (5), PlayEntitlementTest (2),
ReleaseEntitlementsTest (2) e LaunchProductionAuditTest (1).
Cobertura focada: BuildConfig/permissões/metadados/atividades, getters
comerciais bloqueados, demos inativas, pacotes 3000 créditos/100 gemas e
12000 créditos/500 gemas, saldo insuficiente, licença por 300 gemas sem
cobrança repetida, concorrência de saldos, direitos e lifecycle inicial.
Não é uma execução de toda a suíte nem uma validação visual do Compose.

Como regressão adicional, `testDebugUnitTest` terminou com sucesso:
102 testes em 24 classes, zero falhas e zero ignorados. Os relatórios XML
ficaram em `app/build/test-results/testDebugUnitTest/`. Essa suíte não foi
repetida depois de concluída.

Reprodução em ambiente com Android SDK e JDK 21 para testes:

```powershell
.\gradlew.bat :app:compileItchBetaKotlin :app:processItchBetaMainManifest
.\gradlew.bat :app:testItchBetaUnitTest --tests com.example.PublicBetaConfigTest --tests com.example.SaveSafetyTest --tests com.example.PlayEntitlementTest --tests com.example.ReleaseEntitlementsTest --tests com.example.LaunchProductionAuditTest
.\gradlew.bat :app:assembleItchBeta
```

O keystore release permanente existe fora do repositório, mas a configuração
Gradle depende de KEYSTORE_PATH, STORE_PASSWORD e KEY_PASSWORD locais. O AGP
não registrou validateSigningItchBeta com essas variáveis ausentes. Isso não
constitui aprovação de assinatura. `assembleItchBeta` não foi executado e
nenhum APK assinado foi produzido nesta etapa; a verificação do certificado
e do artefato depende da configuração local das credenciais.
Validação visual/toque da loja em aparelho e assinatura de distribuição não
foram realizadas. Não foi iniciada a Etapa 3.
