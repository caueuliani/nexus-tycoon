# Nexus Tycoon — checkpoint de release itch.io (21/10/2026)

**Decisão atual: NO-GO para distribuição.** A meta de 23/10/2026 não substitui os critérios abaixo. Este checkpoint não é uma autorização de publicação. Branch `updates`, estado-base `b0a7c93`; nenhuma build assinada foi gerada neste fechamento.

| Fase | Estado | Evidência e próxima ação mínima |
| --- | --- | --- |
| 0 — Git | PASS local | Working tree inicial limpa; oito commits em `origin/updates..updates`. Varredura desses patches: zero sequências de 11 dígitos isoladas, marcadores de chave privada ou caminhos de keystore/credenciais. Ref local `nexus-assets-before-3e` não deve ser enviada. Sem push. A varredura por padrão não é prova universal de ausência de segredos. |
| 1 — Privacidade | **FAIL operacional** | Exclusão manual de uma conta descartável e seu documento foi confirmada, com segunda conta preservada. O titular confirmou que opera `contatodevtecs@gmail.com`, mas não houve ensaio por essa caixa, teste de sessão ativa ou falha real/repetição. Runbook atualizado com esses casos. Política permanece rascunho, sem URL estável; não prometer fluxo homologado. O titular informou que as regras publicadas no Console coincidem com `firestore.rules`; faltou leitura independente do projeto implantado. |
| 2 — Assets/licenças | **PENDENTE jurídico/documental** | Zero referências de produção aos quatro PNGs inválidos após `b0a7c93`; a abertura da cápsula continua desenhada em Compose. Origem geral das imagens por IA foi declarada pelo titular. Os nomes/retratos inspirados em pessoas reais, sobretudo uma pessoa viva, exigem decisão jurídica humana antes do beta público. Inventário detalhado em `itch-beta-visual-assets-inventory.md`. A lista efetiva de dependências e avisos de terceiros do APK ainda não foi concluída. |
| 3 — Assinatura | **FAIL operacional** | `nexus-tycoon-release.p12` existe fora do Git e o script de geração usa alias `upload`, igual ao Gradle. O titular confirmou que **ainda não fez uma segunda cópia protegida**. A integridade criptográfica e correspondência dos fingerprints não foram verificadas com a senha; não pedir nem registrar senha no chat. `applicationId` permanece `com.aistudio.idletycoon.nxtyc`, `versionCode=1` e `versionName=1.0-itch-beta.1`, adequados como primeira versão beta se não houver APK público anterior com o mesmo código. Confirmar esse histórico antes de assinar. |
| 4 — APK/homologação | NÃO INICIADA | Sem assinatura ou APK `itchBeta` final neste checkpoint. A compilação Kotlin e dois testes focados de UI passaram em `b0a7c93`; os 102 testes debug e 14 específicos da variante são anteriores e não substituem homologação do artefato final. Não instalar sobre o debug do POCO: o `applicationId` é igual e a assinatura difere. Usar ambiente/perfil isolado autorizado para instalação limpa, login, backup, offline, cápsulas e ausência de monetização. |
| 5 — Página itch.io | NÃO INICIADA | Preparar descrição, hash, instruções, avisos, capturas e URL da política **após** aprovar o APK e o procedimento de privacidade. Não houve upload. |

## Bloqueadores antes de gerar um candidato publicável

1. **P0 — privacidade:** operar e ensaiar o pedido de exclusão pelo canal oficial com conta descartável, incluindo sessão ainda ativa, estado parcial, tentativa repetida e ausência final de Auth/Firestore. Uma conta Auth excluída perde o refresh token, mas o ID token já emitido pode persistir até expirar; não afirmar desconexão imediata. Confirmar como impedir ou detectar regravação durante essa janela. Revisão jurídica deve aprovar o procedimento manual e a política.
2. **P0 — continuidade da assinatura:** criar uma cópia **protegida e separada** da keystore e registrar a senha em gerenciador de senhas, verificando que a cópia pode ser lida. Não copiar para Git, chat ou serviço público. Sem isso, perder o único arquivo pode impedir atualizações da beta e uso da mesma assinatura em outra loja.
3. **P1 — risco de imagem/nome:** avaliação humana do uso comercial das caricaturas e trocadilhos associados a pessoas reais; decidir eventual ajuste mínimo, sem remover personagens por iniciativa técnica.
4. **P1 — distribuição documental:** confirmar identificação legal exigida para a página de oferta, faixa 13+, práticas de retenção/atendimento, URL estável da política e notices de licenças dos componentes efetivamente empacotados. O backup automático Android está ativado com regras XML genéricas; refletir isso na versão pública e decidir se precisa restrição específica da variante.

## Configuração técnica já preparada, ainda sem artefato final

- `itchBeta` herda assinatura release, define `debuggable=false`, bloqueia a criação dos controladores Billing/AdMob e oculta as interfaces comerciais. Trocas com moedas internas permanecem. Os SDKs comerciais **ainda estão no grafo de dependências**; não declarar ausência deles no APK sem inspeção do artefato.
- O manifesto da variante remove permissões/atividades/metadados comerciais conforme a verificação anterior; repetir no APK assinado, porque somente o manifesto final comprova a entrega.
- A N.O.V.A. permanece textual. Não existem arquivos de dublagem final.
- Os quatro PNGs inválidos permanecem no repositório por recuperação histórica, mas o código de produção não os referencia. A remoção física é opcional e pode ser feita separadamente.

## Sequência mínima quando os bloqueadores forem resolvidos

1. Confirmar backup protegido da keystore e digitar credenciais **somente em terminal local**, sem eco nem arquivo versionado. Conferir certificado/fingerprints públicos; cadastrar SHA-1/SHA-256 no Firebase apenas se necessário para o login dessa assinatura e após conferir o app/projeto corretos.
2. Gerar `itchBeta` assinada, verificar `debuggable=false`, `applicationId`, versão, manifesto, assinatura, tamanho e SHA-256; não usar assinatura debug. Executar os testes afetados e inspeção final do APK.
3. Homologar em instalação limpa isolada: sem login/rede, save/reabertura/offline, login Google, backup/restauração descartável, três cápsulas e revelação, N.O.V.A., ausência de anúncios/compras reais e logs de crash. Preservar o save principal do POCO.
4. Publicar a política em URL estável e conferir texto exibido antes de preparar a página itch.io. Publicação do APK exige autorização separada do titular.
