# Nexus Tycoon — auditoria da candidata

Esta é uma candidata Android para demonstração técnica e avaliação de publisher, não uma release aprovada para a Google Play. O APK, commit exato, hash e resultados finais estão no arquivo `BUILD-MANIFEST.json` entregue junto ao APK. Nenhuma métrica de jogadores, retenção, receita ou desempenho comercial foi medida nesta auditoria.

## A. Disponível para demonstração

- Jogo Android nativo, Kotlin/Jetpack Compose, com progresso local Room e funcionamento principal sem conta Google.
- Produção industrial, compra múltipla, insumos, automação, mercado, ciência, cientistas, missões, ascensão e Laboratório Estelar implementados; cobertura automatizada de integridade descrita em `progress-integrity.md`.
- Quatro setores de combate, habilidades, cancelamento e recompensas transacionais, com regressões automatizadas.
- Login Google via Credential Manager/Firebase e backup Firestore implementados. A configuração local corresponde ao projeto e pacote esperados. Isso não substitui teste real nesta candidata.
- Anúncio recompensado integrado ao SDK e UMP; debug usa anúncios de teste. Há registro durável de recompensa única e confirmação após fechamento. Exibição real nesta candidata ainda depende de aparelho.
- VIP e licença por dinheiro são demonstrações gratuitas explicitamente identificadas no debug; a concessão de demonstração está bloqueada no release. Guildas e ranking são simulações locais identificadas na interface.

Condição para apresentação externa: executar o roteiro abaixo em aparelho, conservar um backup e explicar as limitações comerciais e sociais. Os testes de Activity são Robolectric, não uma revisão visual em telefone.

## B. Bloqueadores de publicação

1. **Compras reais ausentes:** não há BillingClient, produtos Play, confirmação/consumo de compras, restauração de direitos nem tratamento de renovação/cancelamento de assinatura. O booleano VIP do save é um benefício local, não prova de pagamento. A integração de Billing deve validar compras antes de conceder direitos e confirmar a entrega, conforme [documentação Google](https://developer.android.com/google/play/billing/integrate).
2. **Assinatura/loja:** faltam chave e variáveis de assinatura de produção, conta/configuração Play Console confirmada, bundle assinado, trilhas de teste e revisão da loja. Não foi criada chave fictícia nem usada a debug como produção. O titular deve atender às [exigências de teste aplicáveis à conta](https://support.google.com/googleplay/android-developer/answer/14151465?hl=pt-BR) e configurar [Play App Signing](https://developer.android.com/studio/publish/app-signing).
3. **Privacidade e conta:** não há fluxo de exclusão de conta Firebase/backup nem URL pública de solicitação; falta política de privacidade publicada e acessível. Logout não exclui dados. Preparar declaração Data Safety, retenção e exclusão de acordo com os SDKs reais e [requisitos de exclusão](https://support.google.com/googleplay/android-developer/answer/13327111?hl=pt-BR) / [dados do usuário](https://support.google.com/googleplay/android-developer/answer/10144311?hl=pt-BR).
4. **Firebase real:** não foi confirmado o deploy das regras auditadas. O histórico da conversa continha regras públicas; até conferir o console, não assumir proteção em produção. `firestore.rules` protege `users/{uid}` contra outros usuários, mas aceita o conteúdo enviado pelo proprietário: não é validação financeira ou antitrapaça no servidor.
5. **Monetização por anúncios:** confirmar UMP, faixa etária 13+, conta/app AdMob, elegibilidade e app-ads.txt. Validar ausência de anúncio, erro de rede, retorno do anúncio e recompensa no aparelho; demonstração de SDK não comprova aprovação comercial do AdMob.
6. **QA mobile:** falta cobertura instrumentada funcional, atualização real entre APKs, morte do processo, restauração Firebase real, Android mínimo/atual, desempenho, memória e aparelhos de diferentes fabricantes. O teste instrumentado existente apenas confere o nome do pacote.
7. **Continuidade do progresso:** eventos/guildas simulados mantêm progresso parcial em memória; resgates reais e moedas são persistentes. Antes de oferecer eventos como conteúdo persistente, definir e implementar sua continuidade. Backup substitui um snapshot, não funde progresso de dois dispositivos. Direitos pagos não devem ser confiados ao snapshot do cliente.

## C. Melhorias opcionais para apresentação

- Tratar os avisos de lint restantes, recursos de acessibilidade, textos, contrastes e APIs descontinuadas.
- Revisar dependências de rede aparentemente não utilizadas; não houve atualização ampla de SDKs nesta etapa.
- Avaliar R8/otimização após definir regras e validar release: a configuração atual desativa minificação.
- Persistir histórico de mercado se quiser paridade monetária com oscilações; hoje offline usa preço-base e online usa preço variável, deliberadamente.
- Planejar limite/arquivamento de recibos de anúncios: o documento Firestore cresce com eles.
- Revisar branding definitivo, screenshots, vídeo curto e página de apresentação. Não há build iOS neste repositório.

## Identidade, configuração e segurança

| Item | Constatação |
|---|---|
| Nome | Nexus Tycoon |
| Pacote/applicationId | `com.aistudio.idletycoon.nxtyc` |
| Namespace Kotlin | `com.example`; não é o identificador de instalação |
| Versão | `versionName=1.0`, `versionCode=1`; definir sequência crescente para uploads futuros |
| Android | minSdk 24; targetSdk 36; compileSdk 36.1 |
| Ícones | Arte vetorial existente; adaptativo no API 26+, fallback vetorial no API 24–25. Dez WebP legados tinham RIFF incompatível com o tamanho real e foram removidos. |
| Debug | Depurável, assinatura de desenvolvimento, anúncios oficiais de teste; mesmo applicationId do release |
| Release | Não depurável, IDs AdMob de produção, sem minificação, assinatura externa não configurada; eventual APK unsigned não é instalador para a loja |
| Firebase | Projeto `nexus-idle-tycoon`; config local não versionada, pacote correspondente e cliente OAuth web presente |
| Permissões mescladas | INTERNET, ACCESS_NETWORK_STATE, READ_BASIC_PHONE_STATE, AD_ID, READ_GSERVICES, WAKE_LOCK, FOREGROUND_SERVICE e permissão interna de receiver |
| Componentes exportados | Launcher, activities de autenticação Firebase e componentes dos SDKs; serviços/receivers administrativos têm permissões de proteção. Preview/Activity de tooling aparecem apenas no debug. |
| Transporte | Sem URLs HTTP de backend no código do jogo; sem opt-in de cleartext no manifesto. Firebase/SDKs gerenciam sua comunicação. |
| Backup Android | `allowBackup=true`; XMLs de extração são modelos amplos. Revisar exclusões de preferências de consentimento/sessão e política de transferência antes da loja. |
| Logs | Erros de banco/login/anúncios e detalhes de anúncio em debug. Não foram encontrados logs explícitos de tokens, senhas ou chaves; revisar o logcat real antes da distribuição. |
| Segredos Git | Varredura de padrões nos 142 blobs do histórico disponível, antes dos ajustes desta auditoria, sem correspondências de chave privada, tokens GitHub, chave AWS, service account ou senha literal. Não é prova de ausência universal de segredo nem auditoria de todas as refs remotas. |

Como debug e release compartilham o pacote, uma assinatura diferente pode impedir instalação por cima. **Não desinstalar um progresso importante apenas para resolver conflito de assinatura**: fazer backup e validar o caminho de migração primeiro. Demonstrações salvas não devem ser consideradas direitos comprados.

## Caminhos Firebase e compatibilidade de progresso

O app grava com `set(payload)` e lê com `get(Source.SERVER)` em `users/{FirebaseAuth.uid}`. Não usa email como chave de autorização e não faz listagem de usuários. As regras permitem get/create/update/delete ao próprio UID, negam listagem, acesso de outro UID, acesso anônimo e caminhos extras. Os testes locais não fazem deploy.

| Save/backup | Comportamento |
|---|---|
| Room 8, 9, 10 | Migrações existentes até 11, sem fallback destrutivo |
| Room 11 | Estado atual; não houve mudança de esquema nesta auditoria |
| Backup 4 | Progresso, laboratório e recibos de anúncios; restauração validada e transacional |
| Backup 3 | Mantém laboratório; não reconstrói recibos que não existiam no formato |
| Backup 2 | Laboratório zero e saldo disponível igual aos cores históricos, como compatibilidade já implementada |
| Sem versão, v1, futuro ou inválido | Recusado antes de substituir progresso |

Ganhos offline usam a mesma regra por segundo de insumos/produção/excedente; limite 8h normal/12h VIP, mais até 4h do laboratório. Relógio retrocedido não reabre intervalo pago. Ascensão preserva melhorias permanentes. Combate é encerrado na saída da arena/segundo plano e não ocorre offline. A restauração invalida batalhas pendentes.

## Roteiro manual — 28 minutos

Usar aparelho/conta de teste. Anotar versão, modelo, Android, saldo, níveis e horário. Não usar um progresso insubstituível para testes de troca de assinatura.

| Minutos | Ação | Resultado a observar |
|---|---|---|
| 0–3 | Instalar o APK demo, conferir ícone/nome e abrir sem login | Tutorial e jogo acessíveis; início sem bônus indevidos; sem crash |
| 3–6 | Comprar indústria x1/x5/MAX, produzir manualmente, ativar automação quando houver saldo; vender no mercado | Débito consistente, estoque consumido, sem saldo negativo; botões com números compactos legíveis |
| 6–8 | Comprar ciência/cientista ou coletar missão disponível; tocar rapidamente | Apenas benefícios pagos/resgatáveis; sem recompensa duplicada |
| 8–11 | Entrar na arena, usar escudo/laser/cura; sair durante rodada, voltar e lutar | Efeitos persistem; saída cancela; nenhuma recompensa tardia; setor certo avança |
| 11–14 | Registrar saldos, colocar app em segundo plano, esperar ~1 minuto; retornar; forçar parada e reabrir | Progresso preservado, sem combate offline, sem duplicação do mesmo intervalo |
| 14–17 | Desligar internet, produzir/comprar; tentar login/backup/anúncio; reconectar | Jogo local continua, falhas compreensíveis, nenhum sucesso de nuvem fictício nem recompensa por falha |
| 17–20 | Selecionar faixa etária real de teste; pedir anúncio de teste, concluir e fechar | +5 gemas uma vez, confirmação após fechamento; fechar cedo não promete prêmio |
| 20–24 | Login Google real, fazer backup, anotar resumo; avançar localmente e carregar backup | Conta correta, restauração consistente do snapshot e laboratório; produção não sobrescreve níveis restaurados |
| 24–26 | Encerrar/reabrir e conferir o que foi restaurado; repetir carregar | Snapshot estável, sem somar cores/recibos/intervalos novamente; logout mantém jogo local |
| 26–28 | Conferir ascensão/laboratório com save legítimo de QA; verificar telas sociais/loja | Gasto de cores não reduz bônus histórico, melhorias persistem; demos claramente identificadas |

Se não houver saldo para desbloquear algo em 28 minutos, registrar **não testado**, não manipular moedas de uma conta real. Preparar previamente um save de QA conquistado pelo jogo para laboratório/ascensão. Testes de longas horas offline, API mínimo e morte do processo durante anúncio exigem sessões adicionais.

## Checklist externo e responsáveis sugeridos

| Configuração/entrega | Responsável principal | Evidência necessária |
|---|---|---|
| Titularidade, verificação, pacote e app na Play Console | Titular das contas / publisher | Console e acesso da equipe confirmados |
| Chave de upload, custódia, recuperação e Play App Signing | Titular + desenvolvedor | Certificados corretos; nenhum arquivo privado no Git |
| Produtos, preços regionais, testes de licença e assinaturas | Titular + desenvolvedor | Compra, pendência, cancelamento, reembolso e restauração testados |
| Google Auth, SHA debug/upload/app-signing e OAuth | Titular + desenvolvedor | Login real com a assinatura que será distribuída |
| Firestore e deploy das regras revisadas | Titular + desenvolvedor | Regras do projeto real conferidas e evidência de isolamento |
| Política de privacidade, exclusão e Data Safety | Titular/publisher + desenvolvedor | URLs públicas, fluxo funcional e declarações compatíveis com SDKs |
| AdMob, rewarded, UMP, app-ads.txt, classificação etária | Titular + desenvolvedor | Conta/app habilitados e teste completo de consentimento/recompensa |
| Classificação de conteúdo, público 13+, países, anúncios e preços | Titular / publisher | Formulários da loja revisados |
| Aparelhos, trilha interna/fechada, relatório de pré-lançamento | Desenvolvedor + publisher | Matriz de QA com evidências e falhas resolvidas |
| Arte, música, marcas e direitos de distribuição | Titular / publisher | Licenças e cadeia de direitos verificadas; não auditadas aqui |
| Pitch, vídeo, orçamento, cronograma e plano de testes | Publisher + titular | Materiais baseados nas funcionalidades verificadas, sem métricas inventadas |

## Inventário factual para futuro pitch

| Funcionalidade | Disponibilidade e limite |
|---|---|
| Gestão industrial espacial | Oito indústrias/recursos, cadeia de insumos, upgrades múltiplos e marcos |
| Automação | Agentes de automação, venda de excedente com licença e recursos manuais |
| Mercado | Preços simulados variáveis e venda de estoque; não é economia entre jogadores |
| Progressão | Ciência, seis cartas de cientistas, missões e ranks |
| Prestígio | Ascensão, cores históricos e bônus passivo |
| Laboratório | Engenharia Industrial, Logística Tática e Reserva Offline permanentes |
| Combate | Quatro setores independentes, quatro unidades, escudo/laser/cura e replay local |
| Persistência | Room, migrações e backup/restauração Firebase com login Google |
| Offline | Produção limitada por tempo e insumos; mercado offline a preço-base |
| Anúncios | Rewarded +5 gemas, consentimento/faixa etária; debug usa testes |
| Personalização | Nome, avatares e temas visuais |
| Social/eventos | Guildas/ranking simulados, tarefas de evento e resgates; não há multiplayer ou backend social |
| Monetização | Trocas com moedas do jogo e demos de benefícios; compras reais ainda não integradas |
| Plataformas/métricas | Android; sem build iOS, base de jogadores, receita ou retenção comprovadas nesta auditoria |

