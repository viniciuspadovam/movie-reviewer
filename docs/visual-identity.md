# Identidade visual — Pós-Créditos

## Conceito

"Pós-Créditos" é o momento em que as luzes da sala se acendem e a crítica começa a ser escrita. Os dois temas contam essa passagem:

- **Sala escura** (tema padrão): fundo mogno quase preto, como madeira e veludo na penumbra; o vermelho das poltronas é o único acento.
- **Jornal da manhã** (tema claro): papel-jornal acinzentado, tinta quase preta e o mesmo vermelho, mais escuro, como uma manchete impressa.

A tipografia é de jornal: uma serif de leitura longa, pensada para notícias. O layout **não** imita a grade de broadsheet (sem colunas densas nem fios por toda parte). Só a voz tipográfica vem do jornal.

**Elemento memorável:** a timeline de sessões na página da obra (1ª sessão, 2ª sessão…) com a evolução da nota. Todo o resto fica quieto e disciplinado.

## Logo

- Símbolo: [assets/logo-mark.svg](assets/logo-mark.svg), uma poltrona de cinema vista de frente, em vermelho veludo.
- Assinatura: [assets/logo.svg](assets/logo.svg), símbolo + "Pós-Créditos" em Newsreader 600 (o texto herda `currentColor`).
- Favicon: o símbolo sozinho.

## Paleta

| Token | Sala escura | Jornal da manhã | Uso |
|---|---|---|---|
| `--color-bg` | `#1A1210` | `#EDEBE6` | Fundo da página |
| `--color-surface` | `#241915` | `#F7F6F2` | Painéis, campos, cards da timeline |
| `--color-text` | `#EFE6D8` | `#1E1A18` | Texto principal |
| `--color-text-muted` | `#A8998A` | `#5E5650` | Metadados, datas, legendas |
| `--color-rule` | `#3A2A23` | `#CFC9BF` | Bordas e divisores |
| `--color-accent` | `#A61E2B` | `#9E1B28` | Fundo de botão primário, símbolo, estrelas |
| `--color-accent-text` | `#E2545E` | `#9E1B28` | Links e texto em vermelho (contraste AA) |
| `--color-on-accent` | `#F5EDE3` | `#F7F6F2` | Texto sobre o vermelho |

Contraste verificado: texto principal acima de 13:1, texto secundário acima de 6:1, `--color-accent-text` sobre o fundo acima de 4,5:1 nos dois temas.

## Tipografia (Google Fonts)

- **Newsreader** (variável, com eixo de tamanho óptico `opsz`): títulos e corpo das reviews. Os títulos usam `opsz` alto e peso 600; o corpo usa peso 400 com `line-height` 1,6.
- **Archivo** (variável, eixo `wdth`): interface (botões, filtros, navegação, metadados), a partir de 87,5% de largura para ficar compacta.
- Sem caixa alta em rótulos e sem fonte monospace.

Escala (base 18px para o corpo, proporção clássica de Bringhurst):

| Token | Tamanho | Uso |
|---|---|---|
| `--text-xs` | 0.8125rem (13px) | Atribuições, rodapé |
| `--text-sm` | 0.875rem (14px) | Metadados, UI secundária |
| `--text-md` | 1rem (16px) | UI |
| `--text-body` | 1.125rem (18px) | Corpo das reviews |
| `--text-lg` | 1.3125rem (21px) | Subtítulos |
| `--text-xl` | 1.5rem (24px) | Títulos de seção |
| `--text-2xl` | 2.25rem (36px) | Título de obra/review |
| `--text-3xl` | 3rem (48px) | Manchete da home |

Largura de leitura: `--measure: 66ch`.

## Espaçamento e forma

- Escala de 4px: `--space-1` 4 · `--space-2` 8 · `--space-3` 12 · `--space-4` 16 · `--space-5` 24 · `--space-6` 32 · `--space-7` 48 · `--space-8` 64 · `--space-9` 96.
- Raio: `--radius-sm` 2px (pôsteres, campos) e `--radius-md` 6px (botões, painéis). Sem sombras genéricas: a separação vem do contraste entre `bg` e `surface`.
- Movimento: só em resposta a ações (abrir spoiler, trocar tema). Respeita `prefers-reduced-motion`.

## Nota

Estrelas de 0,5 a 5 em `--color-accent`, com meia estrela. Em listas, a nota aparece também em texto ("4,5") para leitores de tela e para escanear rapidamente.

## Wireframes

Home (últimas reviews):
```
┌───────────────────────────────────────────────┐
│ [poltrona] Pós-Créditos      Buscar   ☾/☀     │
├───────────────────────────────────────────────┤
│ ┌───────┐  Manchete: última review publicada  │
│ │pôster │  ★★★★½  · assistido em 12/09/2026   │
│ │       │  Primeiro parágrafo da review…      │
│ └───────┘                                     │
├───────────────────────────────────────────────┤
│ [pôster] Título — ★★★½ — trecho…              │
│ [pôster] Título — ★★★★ — trecho…              │
│                     Ver mais                  │
├───────────────────────────────────────────────┤
│ rodapé + atribuição TMDB                      │
└───────────────────────────────────────────────┘
```

Página da obra (`/title/:slug`), com o elemento memorável:
```
┌───────────────────────────────────────────────┐
│ backdrop escurecido                           │
│ ┌──────┐  Título (ano) · Filme · gêneros      │
│ │pôster│  Nota atual ★★★★½                    │
│ └──────┘  Sinopse                             │
├───────────────────────────────────────────────┤
│ Sessões                                       │
│  ● 1ª sessão — 03/2019 — ★★★                  │
│  │   trecho…  [ler review]                    │
│  ● 2ª sessão — 08/2024 — ★★★★  (+1)           │
│  │   trecho…  [contém spoiler: mostrar]       │
│  ● 3ª sessão — 09/2026 — ★★★★½ (+½)           │
└───────────────────────────────────────────────┘
```

Página da review (`/review/:id`): cabeçalho curto com a obra e a sessão, texto em uma coluna de `--measure`, "editada em …" quando houver, e links para as outras sessões.

Busca (`/search`): campo de texto em destaque; filtros (tipo, gênero, faixa de nota, ano) em uma linha que vira gaveta no celular; resultados em lista igual à da home.

Login (`/login`): formulário centralizado e estreito, só usuário e senha.

Admin (`/admin/**`): lista de reviews com filtro de status (rascunho/publicada); "Nova obra" com busca no TMDB e resultados com pôster; editor com Markdown e prévia lado a lado (empilhados no celular), seletor de estrelas, data, spoiler e status.
