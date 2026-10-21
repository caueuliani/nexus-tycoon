# Etapa 3 — privacidade e licenciamento (auditoria documental)

Escopo: código e arquivos versionados em `updates` após `660b4cd`, 20/10/2026. Sem build, sem consulta ao Firebase de produção, sem leitura de dados de jogadores e sem teste em aparelho. A política proposta está em `privacy-policy-itch-beta-draft.md` e **não está publicada**.

Identificação pública proposta a partir das informações do responsável em 20/10/2026: **Caue Felipe Uliani da Silva**, nome comercial **DevTecs**, canal público de suporte/privacidade/exclusão **contatodevtecs@gmail.com**. O sufixo numérico da razão social informada anteriormente é o CPF do titular: tratá-lo como dado pessoal e não o reproduzir nesta política, no material promocional ou na página itch.io sem necessidade legal confirmada. Conservar a razão social oficial exata nos cadastros e documentos que a exigirem; nenhuma alteração cadastral foi realizada.

**Fundamento e limite da recomendação:** a [ANPD](https://www.gov.br/anpd/pt-br/assuntos/titular-de-dados-1/direito-dos-titulares) orienta fornecer identificação e contato do controlador, mas essa orientação não exige publicar o CPF completo no aviso de privacidade. A [ANPD](https://www.gov.br/anpd/pt-br/assuntos/titular-de-dados) classifica CPF como dado pessoal. O [Decreto 7.962/2013](https://planalto.gov.br/ccivil_03/_ato2011-2014/2013/decreto/d7962.htm) pode exigir nome empresarial, número de inscrição no CPF/CNPJ e endereço físico/eletrônico em destaque no meio usado para oferta ou contratação de consumo. Portanto, a omissão do CPF **da política de privacidade** é recomendável para minimizar exposição, mas não comprova, por si, a conformidade da futura página de distribuição. Conferir CNPJ e dados cadastrais atuais e obter avaliação jurídica sobre a incidência do decreto ao beta gratuito e sobre o local adequado para cada identificação.

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

**Menor procedimento proposto, ainda não implementado nem prometido ao público:** receber pedidos em `contatodevtecs@gmail.com`; registrar e acompanhar cada pedido sem incluir credenciais ou backup completo no e-mail; verificar de forma proporcional a titularidade da conta; localizar o UID correspondente; apagar o documento `/users/{uid}` e a conta Firebase Auth, confirmando cada resultado e tratando falhas parciais; explicar separadamente o que acontece com dados locais e com o backup Android; responder ao solicitante pelo canal escolhido. Não usar e-mail como único identificador nem apagar dados com base apenas em uma mensagem recebida. Antes de adotar o procedimento, definir responsável pela execução, critérios de verificação, modo de lidar com reautenticação, falhas e retenções legais aplicáveis; testar com conta descartável, nunca com o save real. Se o beta mantiver login/backup, o procedimento operacional deve existir antes da publicação. A adequação jurídica dessa via e a eventual necessidade de exclusão no próprio app precisam de revisão.

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

1. **Recebidos:** nome civil, nome comercial e e-mail público acima. Conferir CNPJ vigente, razão social cadastral e eventual endereço/identificação exigidos na página de oferta; definir onde a política ficará acessível. Não copiar o CPF do registro antigo para páginas públicas por padrão.
2. Procedimento real de atendimento e exclusão: quem executa, como verifica titularidade, como lida com Auth/Firestore, dados locais e backup Android; critérios de conservação que serão adotados. Não inventar prazo.
3. Confirmar se Google Sign-In/backup permanecerão no beta itch, qual projeto Firebase usará e se as regras versionadas estão implantadas.
4. Comprovar origem/licença de cada arte herdada ou autorizar substituição. Verificar especialmente retratos de figuras reconhecíveis e banners/cápsulas.
5. Aprovar faixa 13+, texto e processo para adolescentes após revisão jurídica; confirmar se haverá suporte e coleta operacional de logs.

## Bloqueios

- **P0:** responsável e canal estão identificados, mas o procedimento de pedidos/exclusão ainda não existe operacionalmente; a política permanece rascunho e não deve ser publicada como final.
- **P0:** direito de uso comercial dos retratos, banners e cápsulas herdados não comprovado; não distribuir esses assets sem resolver a procedência.
- **P0:** não foi verificada a configuração Firestore efetivamente implantada, embora o app ofereça login e backup reais. Confirmar antes de disponibilizar o beta com nuvem.
- **P1:** backup automático Android declarado com regras genéricas; definir e testar escopo, divulgação e implicações para exclusão.
- **P1:** SDKs de anúncios/Billing ainda empacotados como dependências compartilhadas, apesar de UI/controladores desativados; conferir APK final e preferir removê-los da variante em etapa técnica própria se isso for necessário para precisão da política/minimização.
- **P1:** faixa 13+ requer revisão operacional/jurídica do tratamento de adolescentes; não há verificação de idade no código.
- **P1:** o CPF constou em commits anteriores da documentação. Removê-lo dos arquivos atuais não o elimina do histórico Git já compartilhado; verificar a visibilidade do repositório e planejar eventual saneamento do histórico com coordenação, sem reescrita unilateral nesta etapa.
- **P2:** gerar avisos de licenças de terceiros e revisar logs; documentar detalhes de geração da arte N.O.V.A. e titularidade dos efeitos procedurais.

## Mínimo para iniciar a Etapa 4

Revisar a política agora identificada com o procedimento executável de exclusão; resolver ou substituir as artes sem prova; verificar regras Firebase implantadas e escopo de backup Android; decidir permanência de login/nuvem no beta; conferir dependências e licenças do artefato final. Estas são recomendações, não alterações realizadas nesta etapa. Revisão jurídica e operacional permanece necessária.
