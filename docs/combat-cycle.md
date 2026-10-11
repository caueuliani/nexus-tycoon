# Ciclo de combate e habilidades

## Bugs confirmados

- O laço guardava `current` entre rodadas, enquanto escudo, laser e cura escreviam `liveCombatStats`. A rodada seguinte podia ignorar ou sobrescrever seus efeitos.
- A vitória por laser disparava uma coroutine no ViewModel, fora do trabalho cancelado ao fechar a arena.
- As habilidades conferiam cooldown, mas não exigiam uma batalha em execução; era possível alterar o estado depois do resultado.
- Não havia cancelamento associado à saída da aba ou ao app ficar oculto.
- O botão de replay iniciava um novo combate, em vez de apresentar o registro anterior.
- Na derrota, a cópia final podia omitir o dano causado ao inimigo na última rodada.

## Correções e regras preservadas

CombatEngine é o único escritor do estado vivo da batalha. Rodadas e habilidades usam esse estado atualizado dentro de transições serializadas. Não há leitura de HP antigo após o atraso da rodada. A interface recebe StateFlows somente de leitura.

Mantidos: intervalo de 1500 ms; dano normal aleatório 85%–115%; retaliação 90%–110%; escudo reduz dano em 80% na próxima retaliação e é consumido nela; laser causa 2× ataque; cura recupera 40% do HP máximo, limitada ao máximo. Cooldowns continuam 10/14/18 segundos para escudo/laser/cura, com o contador de um segundo existente. Inimigos, unidades e recompensas não foram rebalanceados nesta alteração.

O início reserva a sessão antes da operação assíncrona. Uma segunda tentativa não cria outra batalha. Ao terminar, o estado passa imediatamente para Resolving: novas habilidades e rodadas ficam bloqueadas antes de aguardar o banco. Tanto laser quanto rodada passam por esse caminho. A concessão continua em GameRepository.completeCombat, transacional e protegida pelo registro único da batalha.

Cada sessão possui seu próprio trabalho de preparação, rodadas e confirmação de resultado. Fechar invalida a geração e cancela todos esses trabalhos. Uma limpeza posterior só pode marcar o registro como encerrado sem vitória; nunca concede recompensa. Pagamentos ainda aguardando transação são cancelados. Um pagamento já confirmado no banco antes do fechamento permanece creditado; não é desfeito. Se o processo for encerrado durante preparação, um registro pendente pode permanecer, mas uma nova batalha o invalida e não há trabalho retomado para pagá-lo.

## Visibilidade, retorno e cooldowns

- Fechar a arena, usar Voltar durante combate ou sair da aba: cancela a batalha.
- ON_STOP do ciclo de vida (app oculto, tela bloqueada ou Activity substituída): cancela a batalha.
- Retornar ao app ou recriar a Activity: não retoma a batalha. O jogador inicia outra manualmente; não existe combate offline.
- Os cooldowns pertencem ao ViewModel, não à sessão. Persistem ao fechar/reabrir arena e ao girar a tela com ViewModel retido. Continuam decrementando enquanto a coroutine do ViewModel for executada, inclusive sem batalha. Não há compensação por tempo suspenso pelo sistema; morte do processo cria um ViewModel novo e reinicia os cooldowns, como antes.
- O replay é uma cópia dos logs e do resultado confirmado, com cancelamento identificado separadamente de derrota. O botão abre esse registro e não inicia simulação.

## Testes e limites

CombatEngineTest usa relógio virtual e dano determinístico para verificar escudo por uma retaliação, persistência do laser e da cura, limites de HP, cooldowns, ativações concorrentes, dupla tentativa de início, corrida entre laser/rodada, cancelamento antes da rodada e durante pagamento, preparação interrompida, destruição do escopo e replay da derrota.

CombatRewardsTest também verifica cancelamento de pagamento enquanto uma transação de produção ocupa o banco. Os testes anteriores continuam cobrindo idempotência persistente, concorrência de produção, migrações e preservação de saldos.

Pendente em aparelho: navegar entre abas durante combate; Voltar; Home/bloqueio e retorno; rotação; encerrar/reabrir o processo; executar habilidades perto da virada da rodada e do golpe final. Não foi feita validação visual, de lifecycle real ou de temporização em dispositivo físico.

Validação em 11/10/2026: assembleDebug, testDebugUnitTest e lintDebug concluídos. 48 testes passaram, sem falhas ou erros; 9 são de CombatEngineTest e 7 de CombatRewardsTest. Lint: 0 erros e 77 avisos. Sem validação em aparelho.
