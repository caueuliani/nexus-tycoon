# Nexus Tycoon — política de privacidade (rascunho para itchBeta)

**Versão consolidada para revisão do titular, ainda não publicada.** O ensaio técnico de exclusão e o texto de regras informadas pelo titular estão descritos abaixo com suas limitações. Antes da publicação, resolver as decisões assinaladas entre colchetes e revisar juridicamente a identificação, a retenção, o atendimento de direitos e o tratamento de adolescentes. Última revisão do rascunho: 23/10/2026.

## Quem é responsável

O responsável pelo Nexus Tycoon é **Caue Felipe Uliani da Silva**, que atua sob o nome comercial **DevTecs**. Para suporte, dúvidas e pedidos relativos a dados pessoais, escreva para **[contatodevtecs@gmail.com](mailto:contatodevtecs@gmail.com)**. **[Antes da publicação: conferir no cadastro vigente o CNPJ, a identificação empresarial e os dados adicionais exigidos para a página de oferta, sem expor desnecessariamente o CPF do titular.]**

## Dados tratados e finalidade

O jogo funciona localmente sem vincular uma Conta Google. Ele guarda no aparelho o nome da empresa escolhido no jogo, recursos e moedas virtuais, indústrias, pesquisadores, missões, combate, preferências, eventos de progressão e registros técnicos necessários para impedir concessões repetidas. Esses dados permitem continuar a partida e aplicar as regras do jogo.

Se você optar por **Vincular com o Google**, o aplicativo usa a identidade da Conta Google por meio de Google Identity Services e Firebase Authentication. O app acessa o identificador de usuário do Firebase (UID) e o e-mail da conta para mostrar a sessão e associar o backup. O token de login é usado para autenticação; o código não o adiciona ao documento de backup.

Se você tocar em **Salvar na Nuvem**, o aplicativo envia uma cópia do progresso de jogo ao Cloud Firestore, no documento `users/{UID}`. O documento inclui o nome da empresa, saldos virtuais, progresso de indústrias, pesquisadores, missões e combate, opções e marcadores de recompensas. **Carregar Nuvem** busca esse documento e substitui o progresso local após validação. O backup não é automático pelo botão do jogo; depende de sua ação. O jogo também declara o backup automático do Android. As regras XML atuais são modelos sem exclusões efetivas; por padrão, o Android pode incluir dados do aplicativo, inclusive o progresso local, no backup em nuvem ou na transferência entre dispositivos, conforme a versão do sistema e as configurações do usuário. O operador do jogo não controla a exclusão dessas cópias na conta Google ou em outro aparelho. [Android: Auto Backup](https://developer.android.com/identity/data/autobackup).

O jogo registra localmente preferências de áudio e dados técnicos de funcionamento. Em caso de erro, componentes do aplicativo escrevem mensagens no log Android; a disponibilidade desses logs a outras partes depende do sistema e do ambiente. [Confirmar eventual suporte operacional, recebimento de logs e retenção.]

## Serviços externos e compartilhamento

O login e backup opcionais usam Google Identity Services, Firebase Authentication e Cloud Firestore, serviços da Google. O Android pode oferecer backup de dados do aplicativo pelo sistema. A variante gratuita `itchBeta` não apresenta anúncios, compras com dinheiro real ou telemetria própria de Firebase Analytics/Crashlytics no código examinado. Bibliotecas de AdMob, UMP e Google Play Billing ainda constam como dependências do pacote, mas seus controladores e interfaces comerciais são bloqueados nessa variante; isso não equivale a demonstrar, por si só, ausência de toda inicialização interna dos SDKs. [Confirmar o comportamento do APK final e a lista definitiva de fornecedores antes de publicar.]

As regras Firestore versionadas restringem o documento de backup ao UID autenticado correspondente. Os testes locais dessas regras passaram; o titular informou que o texto publicado no Firebase Console para `nexus-idle-tycoon` coincide com o arquivo versionado. A implantação não foi consultada independentemente nesta etapa. A transmissão, infraestrutura e eventual transferência internacional dependem dos serviços Google e de seus termos aplicáveis. Não vendemos dados de progresso a terceiros no fluxo implementado. [Confirmar práticas operacionais do responsável fora do aplicativo.]

## Conservação e proteção

O progresso local permanece no aparelho enquanto o aplicativo e seus dados forem mantidos, ressalvados os mecanismos de backup/restauração do Android. O backup manual no Firestore permanece associado à conta até que seja removido por procedimento apropriado. **O aplicativo atual não oferece exclusão de conta ou backup na interface.** Um ensaio autorizado confirmou que o operador conseguiu remover o documento e a conta Auth de uma conta descartável, preservando uma segunda conta. O teste de recebimento/resposta pelo endereço público ainda aguarda confirmação; sessões ativas e falhas reais de serviço não foram homologadas. O [procedimento operacional](itch-beta-account-deletion-runbook-draft.md) permanece em preparação. Não há prazo operacional de retenção definido; esse ponto precisa ser decidido antes da publicação. O código valida backups antes da restauração e as regras Firestore versionadas restringem o acesso por UID, mas isso não substitui uma verificação das regras implantadas e das medidas operacionais.

Sair da conta encerra a sessão no aplicativo; **não apaga** os dados locais, o backup em nuvem ou a conta Firebase. Desinstalar o app pode remover dados locais, mas não constitui pedido de exclusão do backup em nuvem.

## Seus direitos

Você pode solicitar confirmação do tratamento, acesso, correção, informação sobre compartilhamento, portabilidade quando aplicável, revisão de decisões automatizadas quando cabível e exclusão nos casos previstos pela LGPD. Para exercer esses direitos, escreva para **[contatodevtecs@gmail.com](mailto:contatodevtecs@gmail.com)**. O responsável deverá verificar a titularidade por desafio enviado ao e-mail cadastrado na conta, localizar os dados vinculados à conta e informar o resultado ou uma justificativa aplicável. **Esse procedimento ainda precisa ser homologado pelo canal oficial e em cenário de sessão ativa/falha parcial antes da publicação.** A orientação sobre direitos dos titulares está disponível na [ANPD](https://www.gov.br/anpd/pt-br/assuntos/titular-de-dados-1/direito-dos-titulares).

## Adolescentes

O público pretendido é de 13 anos ou mais, incluindo adolescentes. O jogo não implementa verificação de idade nem um fluxo específico de responsável legal. O tratamento de dados desse público deve observar seu melhor interesse; a adequação da faixa etária, linguagem e eventual mecanismo adicional precisa de avaliação antes da distribuição. A [ANPD](https://www.gov.br/anpd/pt-br/assuntos/noticias/anpd-divulga-enunciado-sobre-o-tratamento-de-dados-pessoais-de-criancas-e-adolescentes) orienta que o tratamento de dados de crianças e adolescentes observe as hipóteses legais da LGPD e seu melhor interesse.

## Alterações desta política

Mudanças relevantes nesta política deverão ser comunicadas por canal apropriado antes de passarem a reger novas formas de tratamento. **[Definir onde a política ficará publicada e como as atualizações serão comunicadas.]**
