# Etapa 3 — privacidade e licenciamento (auditoria documental)

Escopo: código e arquivos versionados em `updates` após `660b4cd`, 20/10/2026. Sem build, sem consulta ao Firebase de produção, sem leitura de dados de jogadores e sem teste em aparelho. A política proposta está em `privacy-policy-itch-beta-draft.md` e **não está publicada**.

## Mapa de dados

| Categoria | Armazenamento/transmissão confirmados pelo código | Finalidade e ressalvas |
| --- | --- | --- |
| Login Google | Credential Manager obtém token Google; Firebase Authentication cria sessão. `CloudSaveManager` lê UID e e-mail e exibe e-mail no Perfil. | Login é opcional. O código não pede senha nem grava token no backup Firestore. A coleta técnica do provedor não foi medida em rede. |
| Room 15 | `game_state`, `resources`, `buildings`, `combat_units`, `researchers`, `active_missions`, `combat_battles`, `ad_reward_receipts`, `play_purchase_receipts`. | Guarda progresso, nome da empresa, recursos, escolhas, patentes, ciclos, marcadores N.O.V.A., identificadores técnicos de batalha e recibos. Recibos comerciais legados podem permanecer em saves antigos. |
| Preferências/arquivos locais | `nexus_audio` SharedPreferences; `ad_privacy` existe no controlador de anúncios, mas este não é construído na itchBeta. Modo sandbox de backup é limitado a debug. | Volume/mute persistentes. O arquivo de demonstração não é o backup real do beta. |
| Firestore | Somente por ação Salvar na Nuvem: `.set(payload)` em `/users/{uid}`; Carregar Nuvem lê do servidor. Payload v7 inclui estado, estoques, indústrias, pesquisadores, missões, unidades e recibos de anúncio. | Documento não contém campo de e-mail/UID dentro do payload; o caminho usa UID. A restauração valida e substitui o save local. Não há sincronização automática pelo botão. |
| Backup Android | Manifesto declara `allowBackup=true`; `data_extraction_rules.xml` e `backup_rules.xml` ainda contêm modelos sem inclusões/exclusões específicas. | Pode abranger dados locais via mecanismo do Android/Google. Conteúdo e comportamento reais devem ser conferidos no APK/dispositivo; não prometer que fica só no aparelho. |
| Logs | `GameViewModel`, `CloudSaveManager` e `GameAudio` escrevem falhas no log Android. | O código não implementa upload próprio de logs; exceções podem incluir detalhes de SDK. Revisar mensagens antes da distribuição. |
| Analytics/ads/compras | Não há dependência de Firebase Analytics/Crashlytics nem emissão de eventos encontrada. AdMob/UMP/Billing permanecem no grafo de dependências compartilhado; na itchBeta os controladores não são criados e UI, permissões e atividades comerciais são removidas. | Ausência de chamadas do app não comprova ausência de coleta interna/transitiva dos SDKs. Confirmar conteúdo e comportamento do APK final. Sem anúncios ou compra real nessa variante. |

`firestore.rules` versionado permite get/create/update/delete somente do documento cujo UID coincide com `request.auth.uid`, nega listagem e demais documentos. **Não foi verificado se as regras implantadas são idênticas.** Login sem conexão não é necessário para o jogo local; backup requer rede e autenticação.

## Exclusão e atendimento

`logout()` apenas chama `FirebaseAuth.signOut()`, limpa o estado de credencial e a indicação de e-mail. Não há função/UI de exclusão de usuário Firebase, documento Firestore ou Room. As regras Firestore permitiriam ao próprio UID excluir seu documento, mas o app não expõe essa operação. Desinstalação não resolve o documento remoto; sair da conta não apaga progresso local. Restauração substitui o save; não é exclusão.

**Menor solução segura proposta, ainda não implementada:** definir responsável/canal operacional; oferecer pedido de exclusão verificável, localizar o UID da sessão e apagar `/users/{uid}` e a conta Auth em ordem controlada, com confirmação e tratamento de falha/reautenticação; decidir separadamente sobre apagar o progresso local e backups Android. Não usar e-mail como único identificador nem exclusão manual sem verificação de titularidade. Registrar o procedimento e testar com conta descartável, nunca com save real. Se o beta permitir login/backup antes disso, ao menos o procedimento humano precisa estar operacional e comunicado; a adequação jurídica dessa alternativa exige revisão.

## Inventário de assets e direitos

| Categoria | Evidência de origem/processo encontrada | Situação para distribuição comercial |
| --- | --- | --- |
| Retratos dos pesquisadores | `ResearcherVisuals.kt` referencia 26 retratos individuais (6 PNG antigos, 20 WebP posteriores) e fallback `img_scientist_card.png`. O histórico Git registra adições, mas não há prompts, masters, licença ou autorização individual na árvore. Alguns nomes/arte antigos evocam pessoas reais. | **Procedência comercial não comprovada.** O usuário informou não possuir documentação dos assets herdados do AI Studio. Confirmar origem e, se necessário, substituir antes do beta. Avaliar nomes/semelhança de pessoas reais. |
| N.O.V.A. | `docs/nova-pilot.md` registra geração original do holograma PNG com ferramenta Codex em 18/10/2026 e ausência de terceiro identificado. | Melhor documentação que as artes antigas; conservar registro de geração/termos aplicáveis e confirmar titular/uso. Não há voz aprovada nem arquivos de dubladores a distribuir. |
| Indústrias, combate e espaço | A ficha industrial usa sobretudo ícones Material/Compose; `img_space_banner.png` e `img_combat_banner.png` são imagens raster herdadas. Não há arquivo de licença/origem por imagem. | Banners: procedência não comprovada. Ícones de biblioteca: conferir avisos de licença na distribuição. |
| Cápsulas e efeitos | Três `img_capsule_*.png` herdados sem documentação individual. A abertura, molduras, brilhos e transições são majoritariamente desenho/animação Compose no código. | Imagens de cápsula: procedência não comprovada. Efeitos em código acompanham titularidade do projeto, sujeita à confirmação dos contribuidores. |
| UI, ícone, fontes | Formas, gradientes e paleta em Compose; ícones Material via AndroidX; launcher em XML vetorial; tipografia usa `FontFamily.Default/Monospace`, sem arquivos em `res/font`. | AndroidX tem licença Apache-2.0 ([fonte do projeto](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt)); preparar avisos de terceiros do conjunto final. Não há licença de fonte externa a provar no repositório. |
| Sons | 14 WAV `sfx_*.wav`; `SOUND_ASSETS.md` declara síntese procedural original e aponta `tools/generate_sfx.py` como fonte reprodutível, sem terceiros. | Evidência interna forte. Confirmar titularidade/autoria do projeto e conservar fonte e declaração. Não há música ou dublagem final. |
| Bibliotecas | Gradle lista AndroidX/Compose/Room, Kotlin, Firebase Auth/Firestore, Google Identity/Credentials, Play Services, AdMob/UMP, Billing, Moshi/OkHttp/Retrofit e dependências de teste. | O código de licença de cada dependência não foi auditado integralmente. Gerar inventário de dependências efetivas e avisos de licença do APK final; verificar termos Google e notices antes de distribuir. |

Histórico Git ou presença de um arquivo **não** demonstra licença comercial. O projeto deriva do Google AI Studio segundo o README, mas isso não determina a proveniência ou direitos de cada imagem. O usuário declarou que não dispõe de comprovação para retratos, banners e cápsulas herdados.

## Informações e decisões pendentes do titular

1. Nome legal do responsável e e-mail público para pedidos de privacidade; o usuário disse que irá informá-los. Confirmar também identificação exigida e canal público da política.
2. Procedimento real de atendimento e exclusão: quem executa, como verifica titularidade, como lida com Auth/Firestore, dados locais e backup Android; retenção que será adotada. Não inventar prazo.
3. Confirmar se Google Sign-In/backup permanecerão no beta itch, qual projeto Firebase usará e se as regras versionadas estão implantadas.
4. Comprovar origem/licença de cada arte herdada ou autorizar substituição. Verificar especialmente retratos de figuras reconhecíveis e banners/cápsulas.
5. Aprovar faixa 13+, texto e processo para adolescentes após revisão jurídica; confirmar se haverá suporte e coleta operacional de logs.

## Bloqueios

- **P0:** responsável/canal e procedimento de pedidos/exclusão ausentes; política ainda contém campos em aberto. Não publicar como política final.
- **P0:** direito de uso comercial dos retratos, banners e cápsulas herdados não comprovado; não distribuir esses assets sem resolver a procedência.
- **P0:** não foi verificada a configuração Firestore efetivamente implantada, embora o app ofereça login e backup reais. Confirmar antes de disponibilizar o beta com nuvem.
- **P1:** backup automático Android declarado com regras genéricas; definir e testar escopo, divulgação e implicações para exclusão.
- **P1:** SDKs de anúncios/Billing ainda empacotados como dependências compartilhadas, apesar de UI/controladores desativados; conferir APK final e preferir removê-los da variante em etapa técnica própria se isso for necessário para precisão da política/minimização.
- **P1:** faixa 13+ requer revisão operacional/jurídica do tratamento de adolescentes; não há verificação de idade no código.
- **P2:** gerar avisos de licenças de terceiros e revisar logs; documentar detalhes de geração da arte N.O.V.A. e titularidade dos efeitos procedurais.

## Mínimo para iniciar a Etapa 4

Preencher e revisar a política com identidade/canal e processo executável de exclusão; resolver ou substituir as artes sem prova; verificar regras Firebase implantadas e escopo de backup Android; decidir permanência de login/nuvem no beta; conferir dependências e licenças do artefato final. Estas são recomendações, não alterações realizadas nesta etapa. Revisão jurídica e operacional permanece necessária.
