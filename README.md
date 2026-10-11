# Nexus Tycoon

Jogo Android de gestão industrial espacial em Kotlin e Jetpack Compose, com banco local Room. Inclui produção automática, mercado, combate, cientistas, missões e prestígio.

**Estado: candidata de demonstração. Não aprovada para produção.** Login Google/Firebase e anúncios recompensados têm integração real implementada, mas exigem validação em aparelho e configuração das contas. Compras reais não estão integradas; guildas e ranking são simulações locais. Consulte a [auditoria para publisher](docs/publisher-candidate.md).

## Compilar e testar

Requisitos: JDK 17 ou 21 para compilação, JDK 21 para os testes Robolectric de Android 16, Android SDK Platform 36.1 e Build Tools 36.0.0. O Gradle Wrapper fixa a versão 9.3.1 e verifica seu checksum.

Abra o projeto no Android Studio ou configure `ANDROID_HOME`/`sdk.dir` em `local.properties`. No Windows:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --continue
```

Em macOS/Linux, use `./gradlew`. O debug utiliza a assinatura padrão de desenvolvimento. O APK local fica em `app/build/outputs/apk/debug/`; o APK antigo de `.build-outputs/` não comprova o estado da revisão atual.

`BackupProductionAuditTest` verifica preservação de progresso após restauração em uma instalação nova. `LaunchProductionAuditTest` verifica a abertura da Activity no Robolectric, sem substituir execução em aparelho real.

O backup atual usa o formato 4 (aceita também 2 e 3), incluindo laboratório e recibos de anúncios. Backups inválidos são recusados antes de alterar dados locais. O modo de demonstração guarda arquivos locais separados por conta e não substitui a nuvem real. A restauração e as operações de progresso foram auditadas e têm regressões de concorrência; consulte [compatibilidade e limitações](docs/progress-integrity.md).

## Publicação

A variante release exige `KEYSTORE_PATH`, `STORE_PASSWORD` e `KEY_PASSWORD`; o alias configurado é `upload`. Mantenha a chave privada e senhas fora do repositório. Gere o bundle com `:app:bundleRelease` somente depois das validações funcionais.

Antes de publicar, implementar Google Play Billing e exclusão de conta/dados, confirmar regras e configurações no Firebase real, validar login/anúncios/atualização/backup em aparelhos, definir persistência dos eventos e concluir as configurações da Play Console. Nenhum teste local comprova aprovação da loja.

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


## Laboratório Estelar

Na tela de Ascensão, Star Cores podem comprar melhorias permanentes. `nebulaCores` continua sendo o total histórico usado nas fórmulas existentes de produção (+10% por core) e requisito de ascensão. `availableCores` é o saldo gasto nas compras; ascensão e eventos creditam ambos os contadores.

- Engenharia Industrial: −2% por nível nos custos em créditos das indústrias, até 10 níveis. Multiplica o preço já ajustado pelos cientistas; não reduz outras moedas.
- Logística Tática: −2% por nível nos custos em créditos das unidades, até 10 níveis.
- Reserva Offline: +1 hora ao limite de 8h normal/12h VIP, até 4 níveis; não altera produção por segundo.
- Custos: 5, 8, 12, 18, 27, 40, 60, 90, 135, 200 para as duas primeiras; 10, 25, 50, 100 para Reserva Offline.

Room 10 migra de 9 sem apagar dados e copia o total histórico para o saldo uma única vez. O caminho 8 → 9 → 10 permanece disponível. Compras usam o nível esperado e uma transação para evitar duplicar a compra exibida por toques rápidos. Gravações genéricas de estado não sobrescrevem a progressão de cores.

Backups v3 incluem saldo, níveis e resgates dos três eventos atuais. Backups v2 continuam aceitos: são restaurados com saldo igual ao total histórico e laboratório no nível zero. Restaurar substitui o snapshot, não soma cores ao estado atual. Backups incompletos mais antigos continuam recusados. Resgates antigos de eventos não existiam no save e não podem ser reconstruídos; o registro durável começa nesta versão. O progresso parcial desses eventos continua em memória.

`StellarLabTest` cobre migração/reabertura, transações concorrentes, concessões, preços compartilhados, persistência no backup e ganhos offline. Não substitui validação visual ou execução em aparelho.

Validação desta implementação (10/10/2026): assembleDebug, testDebugUnitTest e lintDebug concluídos. 32 testes passaram (6 em StellarLabTest), sem falhas. Lint: 0 erros e 77 avisos. Não houve execução em aparelho; continuam pendentes a conferência visual e o teste de atualização/backup no dispositivo real.

## Mineração hostil

O rebalanceamento de créditos/diamantes e sua comparação com custos e indústria estão em [docs/combat-balance.md](docs/combat-balance.md). Room 11 acrescenta registros locais de batalha com migração 10 → 11, preservando o Laboratório Estelar. Recompensas são fixadas no início e pagas uma única vez em transação. A auditoria de integridade posterior atualizou o backup para v4, aceitando v2 e v3.

O ciclo de combate e o comportamento de cancelamento, habilidades, cooldowns e replay estão documentados em [docs/combat-cycle.md](docs/combat-cycle.md).
