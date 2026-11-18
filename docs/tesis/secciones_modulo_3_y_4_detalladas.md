# CAPÍTULO III: MARCO METODOLÓGICO Y DESARROLLO DE LA SOLUCIÓN
## SECCIONES DETALLADAS: MÓDULO 3 Y MÓDULO 4

---

### 3.X. MÓDULO 3: SISTEMA DE RAZONAMIENTO DIFUSO Y COINCIDENCIA LÉXICO-FONÉTICA

El Módulo 3 constituye el núcleo algorítmico y de procesamiento inteligente de la investigación. Su finalidad es corregir los errores que cometen los sistemas de Reconocimiento Automático del Habla (ASR) de propósito general, operando *On-Device* en el aula, sobre el vocabulario curricular. Estos errores se deben sobre todo a términos técnicos poco frecuentes en el modelo de lenguaje del reconocedor y al habla espontánea del docente; la reverberación y el ruido de fondo influyen en menor medida, porque el micrófono de solapa capta la voz a 15–20 cm de la boca. Este módulo desacopla la decisión de sustitución en tres componentes computacionales encadenados:
1. **Filtro Morfosintáctico de Part-of-Speech (POS):** Discrimina términos gramaticales candidatos.
2. **Algoritmo de Similitud Combinada Fonético-Léxica de Levenshtein:** Cuantifica la cercanía morfológica y fonológica hispana con el vocabulario del MINEDU.
3. **Motor de Inferencia Difusa Mamdani:** Modela formalmente la incertidumbre acústica, fonética y semántica (coherencia con el tema de la clase) mediante conjuntos difusos y defuzzificación por Centroide de Gravedad (COG).

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        FLUJO COMPUTACIONAL DEL MÓDULO 3                                │
└────────────────────────────────────────────────────────────────────────────────────────┘
  Palabra transcrita (w_i) 
  + Confianza ASR (C_i, por frase)
            │
            ▼
 ┌──────────────────────┐   NO es candidato   ┌─────────────────────────────────────────┐
 │ 3.1 Filtro POS       │ ──────────────────> │ Conservar palabra original sin cambios  │
 │ (OpenNlpPosTagger)   │                     └─────────────────────────────────────────┘
 └──────────┬───────────┘
            │ SÍ (Sustantivo / Término clave)
            ▼
 ┌──────────────────────┐   D_max < 0.50      ┌─────────────────────────────────────────┐
 │ 3.2 Similitud Léxico-│ ──────────────────> │ Conservar palabra original sin cambios  │
 │ Fonética Levenshtein │                     └─────────────────────────────────────────┘
 └──────────┬───────────┘
            │ SÍ (D_max >= 0.50)
            ▼
 ┌────────────────────────────────────────────────────────┐
 │ 3.3 Motor Difuso Mamdani (JFuzzyEngine)                │
 │  • Fuzzificación: mu(C), mu(D) y mu(S)                 │
 │  • Inferencia: 27 Reglas Mamdani (Operador MIN)        │
 │  • Agregación de Consecuentes (Operador MAX)           │
 │  • Defuzzificación por Centroide de Gravedad (COG)     │
 └──────────────────────────┬─────────────────────────────┘
                            │
                            ▼ Grado de Justificación y* in [0.0, 1.0]
```

---

#### 3.X.0. Ejemplos Prácticos de Razonamiento en Aulas de Secundaria

Para comprender de manera intuitiva y cuantitativa cómo operan conjuntamente los tres componentes del Módulo 3, se presentan a continuación dos casos representativos extraídos del corpus experimental de la investigación en colegios de Arequipa:

##### Caso de Estudio 1: Corrección Justificada de Términos Técnicos en Clase de Biología
- **Verdad de campo (habla oral del docente):**  
  *"la célula vegetal realiza la fotosíntesis dentro del cloroplasto y posee mitocondria"*
- **Transcripción cruda del ASR (términos técnicos deformados):**  
  *"la cédula vegetal realiza la fotosintesis dentro del coroplasto y posee mitocondria"*
- **Confianza acústica reportada por el ASR:** $C = 0.42$ para todo el enunciado. La API `SpeechRecognizer` entrega un único valor por hipótesis, no por palabra, y el prototipo lo asigna a todas las palabras de la oración.

**Evaluación analítica paso a paso de los tokens:**
1. **Palabras descartadas inmediatamente por el Filtro POS:** *"la"*, *"dentro"*, *"del"*, *"y"*.  
   El componente `OpenNlpPosTagger` detecta que son conectores gramaticales o palabras funcionales ($\text{IsCandidateNoun} = \text{false}$). Se conservan intactas en tiempo constante $O(1)$ sin realizar búsquedas en el diccionario ni consumir cómputo difuso.
2. **Evaluación del término dudoso *"cédula"* ($C = 0.42$):**
   - **Filtro POS:** Sustantivo de longitud 6 $\to \text{true}$. Se autoriza la búsqueda en el diccionario.
   - **Similitud Combinada frente al término canónico *"célula"*:**
     - Similitud ortográfica: $L(\text{"cedula"}, \text{"celula"}) = 1 \implies D_{\text{lex}} = 1.0 - (1 / 6) = 0.833$.
     - Similitud fonética: $\Phi(\text{"cédula"}) = \text{"sedula"}$, $\Phi(\text{"célula"}) = \text{"selula"} \implies D_{\text{fon}} = 1.0 - (1 / 6) = 0.833$.
     - Similitud ponderada: $D = 0.60(0.833) + 0.40(0.833) = 0.833 \ge 0.50$ (Supera el umbral de pre-activación).
   - **Coherencia semántica $S$:** los otros términos sólidos de la oración (*fotosíntesis*, *cloroplasto*, *mitocondria*) pertenecen a clusters distintos al de *célula* (Citología General y Transporte Celular), aunque de la misma área temática; el cálculo del prototipo arroja $S = 0.50$ (neutra, sin memoria previa de la clase).
   - **Fuzzificación en `JFuzzyEngine`:**
     - Para $C = 0.42$: $\mu_{\text{Baja}}(0.42) = 0.72$, $\mu_{\text{Media}}(0.42) = 0.28$, $\mu_{\text{Alta}}(0.42) = 0.0$.
     - Para $D = 0.833$: $\mu_{\text{Baja}}(0.833) = 0.0$, $\mu_{\text{Media}}(0.833) = 0.11$, $\mu_{\text{Alta}}(0.833) = 0.55$.
     - Para $S = 0.50$: $\mu_{\text{Baja}}(0.50) = 0.0$, $\mu_{\text{Media}}(0.50) = 1.0$, $\mu_{\text{Alta}}(0.50) = 0.0$.
   - **Inferencia de Reglas (reglas activas, MIN):**
     - Regla 2 ($C$ Baja, $D$ Alta, $S$ Media $\implies$ $J$ Alta): $w_2 = \min(0.72, 0.55, 1.0) = 0.55$.
     - Regla 11 ($C$ Media, $D$ Alta, $S$ Media $\implies$ $J$ Alta): $w_{11} = \min(0.28, 0.55, 1.0) = 0.28$.
     - Regla 5 ($C$ Baja, $D$ Media, $S$ Media $\implies$ $J$ Media): $w_5 = \min(0.72, 0.11, 1.0) = 0.11$.
     - Regla 14 ($C$ Media, $D$ Media, $S$ Media $\implies$ $J$ Media): $w_{14} = \min(0.28, 0.11, 1.0) = 0.11$.
     - Agregación (MAX): $\text{activaAlta} = \max(0.55, 0.28) = 0.55$; $\text{activaMedia} = \max(0.11, 0.11) = 0.11$.
   - **Defuzzificación por Centroide de Gravedad (COG):**
     - Integrando la masa difusa resultante en el universo $y \in [0.0, 1.0]$, se obtiene el valor escalar:
       $$y^* = 0.785$$
   - **Juicio de Decisión:** Como $y^* = 0.785 \ge 0.65$ y $D = 0.833 \ge 0.70$, la sustitución silenciosa es **APROBADA**. El sistema sustituye en el teleprompter *"cédula"* por *"célula"*, recuperando la precisión semántica para el estudiante sordo.
3. **Evaluación del término deformado *"coroplasto"* ($C = 0.42$):**
   - Término más cercano en el diccionario: *"cloroplasto"*.
   - Similitud calculada: $D = 0.909$. Confianza acústica del enunciado: $C = 0.42$.
   - Coherencia semántica $S = 0.833$ (co-ocurre con *fotosíntesis* en el cluster Metabolismo, Biomoléculas y Bioenergética). El centroide difuso es $y^* = 0.836 \ge 0.65 \implies$ **APROBADA**. Se sustituye por *"cloroplasto"*.

---

##### Caso de Estudio 2: Prevención de Falsos Positivos ante Palabras Comunes en Química
- **Verdad de campo:** *"el calor genera una reacción"*
- **Transcripción acústica del ASR:** *"el calor genera una reacción"* (con alta certeza acústica en la palabra común: $C_{\text{calor}} = 0.92$).
- **Riesgo:** En el diccionario curricular de Química figura el término técnico *"cloro"*. Un algoritmo de coincidencia aproximada puramente ortográfico o basado solo en distancia de Levenshtein encontraría una similitud moderada ($D \approx 0.60$) y podría sustituir erróneamente *"calor"* por *"cloro"*, desvirtuando el concepto químico explicado por el docente.
- **Solución mediante Lógica Difusa en el Prototipo:**
  - $C = 0.92 \implies \mu_{\text{Alta}}(C) = 1.0, \mu_{\text{Media}}(C) = 0.0, \mu_{\text{Baja}}(C) = 0.0$.
  - $D = 0.60 \implies \mu_{\text{Media}}(D) = 0.60, \mu_{\text{Baja}}(D) = 0.20, \mu_{\text{Alta}}(D) = 0.0$.
  - $S = 0.20$ (el término *cloro* no está indexado en los clusters temáticos $\implies$ $\mu_{\text{Baja}}(S) = 1.0$).
  - **Inferencia:**
    - Se dispara la **Regla 24** ($C$ Alta, $D$ Media, $S$ Baja $\implies$ $J$ Baja): $w_{24} = \min(1.0, 0.60, 1.0) = 0.60$, y la **Regla 27** ($C$ Alta, $D$ Baja, $S$ Baja $\implies$ $J$ Baja): $w_{27} = \min(1.0, 0.20, 1.0) = 0.20$.
    - Fuerza de Justificación Alta: $\text{activaAlta} = 0.0$.
    - Fuerza de Justificación Baja: $\text{activaBaja} = \max(0.60, 0.20) = 0.60$.
  - **Defuzzificación COG:**
    $$y^* = 0.219$$
  - **Juicio de Decisión:** Dado que $y^* = 0.219 < 0.65$, la sustitución es **RECHAZADA**. La palabra *"calor"* se conserva intacta en pantalla, comprobándose la capacidad del motor difuso para neutralizar falsos positivos cuando el docente se expresa con lenguaje coloquial.

---


##### 3.X.1.1. Justificación Pedagógica y Computacional
En una clase de educación secundaria regular, entre el 60% y el 70% de las emisiones verbales del docente corresponden a palabras funcionales, artículos, preposiciones y conectores gramaticales (*"el", "la", "de", "con", "por qué", "entonces"*). Si el sistema evaluara cada una de estas palabras contra el diccionario académico escolar (con cientos de términos), se incurriría en dos problemas críticos:
1. **Sobrecarga de cómputo ($O(N \cdot M)$):** Búsqueda exhaustiva innecesaria que consumiría tiempo de CPU y batería en el dispositivo móvil, violando el umbral de latencia en tiempo real ($< 1000\text{ ms}$).
2. **Falsos positivos de sustitución:** Palabras de enlace cortas podrían confundirse espuriamente con raíces de términos técnicos (ejemplo: confundir la preposición *"con"* con un fragmento de *"cono"* o *"clon"*).

Por ende, el componente `OpenNlpPosTagger` actúa como un **filtro discriminador de paso previo** que aísla sustantivos y entidades léxicas de interés académico.

##### 3.X.1.2. Reglas de Filtrado y Formalización Matemática
Sea $w$ la palabra candidata entregada por el ASR y $w_{\text{clean}}$ la cadena resultante de aplicar remoción de signos de puntuación y conversión a minúsculas:
$$w_{\text{clean}} = \text{regex\_replace}(w, \text{"[^a-záéíóúüñ]"}, \text{""})$$

El predicado booleano de admisión léxica $\text{IsCandidateNoun}(w) \in \{\text{true}, \text{false}\}$ se define como:
$$\text{IsCandidateNoun}(w) = \begin{cases} 
\text{false}, & \text{si } |w_{\text{clean}}| < 3 \\
\text{false}, & \text{si } w_{\text{clean}} \in \mathcal{S}_{\text{stopwords}} \\
\text{true}, & \text{si } \text{isUpperCase}(w[0]) = \text{true} \\
\text{true}, & \text{si } \exists s \in \mathcal{S}_{\text{sufijos}} \text{ tal que } \text{endsWith}(w_{\text{clean}}, s) \\
\text{true}, & \text{si } |w_{\text{clean}}| \ge 4 \\
\text{false}, & \text{en cualquier otro caso}
\end{cases}$$

Donde:
- **$\mathcal{S}_{\text{stopwords}}$:** Conjunto finito de 75 palabras funcionales del español peruano excluidas de manera directa:
  $$\mathcal{S}_{\text{stopwords}} = \{\text{"el"}, \text{"la"}, \text{"los"}, \text{"las"}, \text{"de"}, \text{"del"}, \text{"en"}, \text{"con"}, \text{"por"}, \text{"para"}, \text{"que"}, \text{"como"}, \text{"cuando"}, \dots\}$$
- **$\mathcal{S}_{\text{sufijos}}$:** Conjunto de 26 sufijos morfológicos altamente característicos de la terminología científica y humanística escolar secundaria:
  $$\mathcal{S}_{\text{sufijos}} = \{\text{"ción"}, \text{"sión"}, \text{"miento"}, \text{"dad"}, \text{"tad"}, \text{"ismo"}, \text{"ista"}, \text{"metría"}, \text{"grafía"}, \text{"logía"}, \text{"soma"}, \text{"plasto"}, \text{"geno"}, \text{"lisis"}, \text{"trofia"}, \text{"sfera"}\}$$

---

#### 3.X.2. Componente 3.2: Algoritmo de Similitud Fonético-Léxica de Levenshtein (`LevenshteinMatcher`)

##### 3.X.2.1. Distancia de Edición Clásica de Levenshtein
Dadas dos cadenas de caracteres $s_1$ (palabra pronunciada/reconocida de longitud $m$) y $s_2$ (término del diccionario académico de longitud $n$), la distancia de edición $L(s_1, s_2)$ representa el número mínimo de operaciones elementales de inserción, eliminación o sustitución de un solo carácter requeridas para transformar $s_1$ en $s_2$.

Se calcula mediante la siguiente relación de recurrencia en una matriz de programación dinámica $dp[i][j]$ para $0 \le i \le m$ y $0 \le j \le n$:
$$dp[i][0] = i, \quad \forall i \in [0, m]$$
$$dp[0][j] = j, \quad \forall j \in [0, n]$$
$$dp[i][j] = \min \begin{cases} 
dp[i-1][j] + 1 & \text{(Eliminación)} \\
dp[i][j-1] + 1 & \text{(Inserción)} \\
dp[i-1][j-1] + \text{costo}(s_1[i], s_2[j]) & \text{(Sustitución)}
\end{cases}$$

Donde:
$$\text{costo}(s_1[i], s_2[j]) = \begin{cases} 0, & \text{si } s_1[i] = s_2[j] \\ 1, & \text{si } s_1[i] \ne s_2[j] \end{cases}$$

La distancia final es $L(s_1, s_2) = dp[m][n]$.

##### 3.X.2.2. Similitud Ortográfica Normalizada ($D_{\text{lex}}$)
Para desacoplar la métrica de la longitud absoluta de las palabras y obtener un índice acotado en el intervalo continuo $[0.0, 1.0]$, se define la similitud léxica normalizada:
$$D_{\text{lex}}(s_1, s_2) = 1.0 - \frac{L(\text{strip}(s_1), \text{strip}(s_2))}{\max(|s_1|, |s_2|)}$$
Donde $\text{strip}(s)$ normaliza la cadena bajo la forma de descomposición canónica Unicode (NFD), eliminando diacríticos y acentos ortográficos. Si $|s_1| = |s_2| = 0$, entonces $D_{\text{lex}} = 1.0$.

##### 3.X.2.3. Función de Normalización Fonológica Hispana ($\Phi$)
La distancia ortográfica pura falla drásticamente al evaluar transcripciones en el aula escolar de Arequipa debido a peculiaridades fonéticas dialectales del español latinoamericano. Por ejemplo, la confusión entre grafemas fonéticamente indistinguibles en el habla continua (seseo, yeísmo, indistinción b/v, hache muda) produce penalizaciones ortográficas de Levenshtein elevadas para palabras que acústicamente son casi idénticas.

Se diseñó la función de transformación fonológica $\Phi(s): \Sigma^* \rightarrow \Sigma^*$ que aplica una serie de sustituciones determinísticas:
1. **Neutralización bilabial:** Todas las instancias de $'v'$ se sustituyen por $'b'$:
   $$v \mapsto b$$
2. **Neutralización del seseo hispano:** Los grafemas $'z'$ y $'c'$ ante vocal palatal ($'e', 'i'$) se neutralizan a $'s'$:
   $$z \mapsto s, \quad ce \mapsto se, \quad ci \mapsto si$$
3. **Neutralización del yeísmo:** La elle ($'ll'$) se transforma en semiconsonante $'y'$:
   $$ll \mapsto y$$
4. **Omisión de grafema mudo:** Se suprime el grafema $'h'$:
   $$h \mapsto \emptyset$$
5. **Neutralización oclusiva velar:** Los grafemas $'qu'$ y $'c'$ (ante vocales $a, o, u$) se homogeinizan a $'k'$:
   $$qu \mapsto k, \quad c \mapsto k$$

##### 3.X.2.4. Similitud Fonética Normalizada ($D_{\text{fon}}$)
Se calcula la distancia de Levenshtein entre las proyecciones fonológicas de ambas cadenas:
$$p_1 = \Phi(s_1), \quad p_2 = \Phi(s_2)$$
$$D_{\text{fon}}(s_1, s_2) = 1.0 - \frac{L(p_1, p_2)}{\max(|p_1|, |p_2|)}$$
Si $\max(|p_1|, |p_2|) = 0$, entonces $D_{\text{fon}} = 1.0$.

##### 3.X.2.5. Similitud Combinada Ponderada ($D$)
La similitud léxico-fonética total $D \in [0.0, 1.0]$ se formula como la combinación lineal convexa:
$$D = \alpha \cdot D_{\text{lex}} + (1 - \alpha) \cdot D_{\text{fon}}$$

En la calibración experimental del prototipo se fijó $\alpha = 0.60$:
$$D = 0.60 \cdot D_{\text{lex}} + 0.40 \cdot D_{\text{fon}}$$

*Justificación del peso 60/40:* Otorga primacía a la corrección ortográfica canónica (60%), garantizando que la sintaxis formal del término se preserve, mientras que el componente fonético (40%) absorbe las confusiones del reconocedor entre grafías que suenan igual en el español peruano (por ejemplo, *c/s/z*, *b/v* o *ll/y*).

##### 3.X.2.6. Umbral de Pre-filtrado ($D \ge 0.50$)
Para optimizar el uso del CPU móvil, la palabra candidata únicamente es remitida al motor difuso si el término del diccionario más cercano satisface:
$$D_{\max} = \max_{t_j \in \mathcal{D}} D(w_i, t_j) \ge 0.50$$
Si $D_{\max} < 0.50$, la palabra se descarta inmediatamente de la corrección, asumiendo que no guarda correlación pedagógica con el vocabulario de la asignatura activa.

---

#### 3.X.3. Componente 3.3: Motor de Inferencia Difusa Mamdani (`JFuzzyEngine`)

##### 3.X.3.1. Arquitectura del Modelo Difuso
El motor difuso Mamdani procesa la incertidumbre derivada de la señal de voz del docente capturada en el aula. Se estructura formalmente como un sistema MISO (*Multiple-Input, Single-Output*) con tres variables lingüísticas de entrada y una de salida:
- **Entrada 1:** Confianza Acústica $C \in [0.0, 1.0]$, provista por el motor ASR nativo. La API entrega un único valor por hipótesis (frase), que el prototipo aplica a todas sus palabras; si falta se usa 0.65 (0.60 en resultados parciales).
- **Entrada 2:** Similitud Léxico-Fonética $D \in [0.0, 1.0]$, provista por el componente `LevenshteinMatcher`.
- **Entrada 3:** Coherencia Semántica $S \in [0.0, 1.0]$, calculada por co-ocurrencia del término candidato con otros términos de la oración en los *topic clusters* de la asignatura y la memoria temática de la clase (0.50 sin evidencia).
- **Salida:** Grado de Justificación de Corrección $y^* \in [0.0, 1.0]$.

##### 3.X.3.2. Definición Formal de Funciones de Pertenencia

###### Definición Estándar de Primitivas Geométricas:
- **Función Triangular $\text{trimf}(x; a, b, c)$:**
  $$\mu(x; a, b, c) = \max \left( 0, \min \left( \frac{x - a}{b - a}, \frac{c - x}{c - b} \right) \right)$$
- **Función Trapezoidal $\text{trapmf}(x; a, b, c, d)$:**
  $$\mu(x; a, b, c, d) = \max \left( 0, \min \left( \frac{x - a}{b - a}, 1, \frac{d - x}{d - c} \right) \right)$$

###### 1. Variable de Entrada: Confianza Acústica ($C \in [0.0, 1.0]$)
Cuantifica la certeza que el reconocedor reporta sobre la hipótesis de la frase. Es un puntaje que calcula internamente el motor ASR y que el prototipo recibe sin modificarlo. Su valor puede bajar por varias causas: vocabulario técnico poco frecuente en el modelo del reconocedor, articulación rápida o poco clara, reverberación del aula y ruido de fondo. El sistema no distingue estas causas ni mide el ruido; usa $C$ como indicador global de la incertidumbre del reconocedor.
$$\mu_{\text{Baja}}(C) = \text{trapmf}(C; 0.0, 0.0, 0.35, 0.60)$$
$$\mu_{\text{Media}}(C) = \text{trimf}(C; 0.35, 0.60, 0.80)$$
$$\mu_{\text{Alta}}(C) = \text{trapmf}(C; 0.65, 0.85, 1.0, 1.0)$$

###### 2. Variable de Entrada: Similitud Léxico-Fonética ($D \in [0.0, 1.0]$)
Cuantifica la cercanía entre la palabra transcrita y el término escolar de referencia:
$$\mu_{\text{Baja}}(D) = \text{trapmf}(D; 0.0, 0.0, 0.40, 0.65)$$
$$\mu_{\text{Media}}(D) = \text{trimf}(D; 0.45, 0.70, 0.85)$$
$$\mu_{\text{Alta}}(D) = \text{trapmf}(D; 0.75, 0.90, 1.0, 1.0)$$

###### 3. Variable de Entrada: Coherencia Semántica ($S \in [0.0, 1.0]$)
Cuantifica qué tan coherente es el término candidato con el tema que se está tratando en la oración y en la clase:
$$\mu_{\text{Baja}}(S) = \text{trapmf}(S; 0.0, 0.0, 0.25, 0.45)$$
$$\mu_{\text{Media}}(S) = \text{trimf}(S; 0.30, 0.50, 0.70)$$
$$\mu_{\text{Alta}}(S) = \text{trapmf}(S; 0.55, 0.75, 1.0, 1.0)$$

###### 4. Variable de Salida: Grado de Justificación de Corrección ($y \in [0.0, 1.0]$)
Representa la certeza matemática de que la palabra original debe ser sustituida por el término del diccionario:
$$\mu_{\text{Baja}}(y) = \text{trapmf}(y; 0.0, 0.0, 0.30, 0.50)$$
$$\mu_{\text{Media}}(y) = \text{trimf}(y; 0.35, 0.55, 0.75)$$
$$\mu_{\text{Alta}}(y) = \text{trapmf}(y; 0.60, 0.80, 1.0, 1.0)$$

##### 3.X.3.3. Base de Conocimiento: Matriz de Reglas de Inferencia Difusa
La base de reglas fue formalizada en el archivo de especificación FCL (*Fuzzy Control Language*, ver [fuzzy_rules.fcl](file:///home/miguel/Projects/Prototipo%20fuzzy/Prototipo-Fuzzy-Transcripcion/app/src/main/assets/fuzzy_rules.fcl)) y codificada en Kotlin nativo para maximizar la velocidad de cómputo en Android:

| Regla | $C$ | $D$ | $S$ | Justificación ($J$) |
|:---:|:---:|:---:|:---:|:---:|
| **$R_{1}$** | Baja | Alta | Alta | **Alta** |
| **$R_{2}$** | Baja | Alta | Media | **Alta** |
| **$R_{3}$** | Baja | Alta | Baja | **Media** |
| **$R_{4}$** | Baja | Media | Alta | **Alta** |
| **$R_{5}$** | Baja | Media | Media | **Media** |
| **$R_{6}$** | Baja | Media | Baja | **Baja** |
| **$R_{7}$** | Baja | Baja | Alta | **Media** |
| **$R_{8}$** | Baja | Baja | Media | **Baja** |
| **$R_{9}$** | Baja | Baja | Baja | **Baja** |
| **$R_{10}$** | Media | Alta | Alta | **Alta** |
| **$R_{11}$** | Media | Alta | Media | **Alta** |
| **$R_{12}$** | Media | Alta | Baja | **Media** |
| **$R_{13}$** | Media | Media | Alta | **Alta** |
| **$R_{14}$** | Media | Media | Media | **Media** |
| **$R_{15}$** | Media | Media | Baja | **Baja** |
| **$R_{16}$** | Media | Baja | Alta | **Media** |
| **$R_{17}$** | Media | Baja | Media | **Baja** |
| **$R_{18}$** | Media | Baja | Baja | **Baja** |
| **$R_{19}$** | Alta | Alta | Alta | **Media** |
| **$R_{20}$** | Alta | Alta | Media | **Media** |
| **$R_{21}$** | Alta | Alta | Baja | **Baja** |
| **$R_{22}$** | Alta | Media | Alta | **Media** |
| **$R_{23}$** | Alta | Media | Media | **Baja** |
| **$R_{24}$** | Alta | Media | Baja | **Baja** |
| **$R_{25}$** | Alta | Baja | Alta | **Baja** |
| **$R_{26}$** | Alta | Baja | Media | **Baja** |
| **$R_{27}$** | Alta | Baja | Baja | **Baja** |

**Racionalidad de la base de reglas:**
- *$C$ Baja o Media con $D$ Alta y $S$ Alta o Media* ($R_1$, $R_2$, $R_4$, $R_{10}$, $R_{11}$, $R_{13}$): el reconocedor duda de su hipótesis (por vocabulario poco frecuente en su modelo, articulación, reverberación o ruido de fondo), pero la palabra se parece mucho a un término técnico y encaja con el tema de la clase. Se justifica la corrección.
- *$S$ Baja* con $D$ Alta o Media ($R_3$, $R_6$, $R_{12}$, $R_{15}$): el término candidato no encaja con el tema de la oración; la justificación baja o es sólo media.
- *$D$ Baja* ($R_8$, $R_9$, $R_{17}$, $R_{18}$, $R_{25}$–$R_{27}$): la palabra no se parece a ningún término del diccionario (palabra común o ajena al dominio); no debe forzarse una corrección espuria. Sólo con $S$ Alta y $C$ Baja o Media ($R_7$, $R_{16}$) la justificación llega a Media.
- *$C$ Alta* ($R_{19}$–$R_{27}$): el ASR tiene alta certeza de lo que oyó, por lo que la intervención es conservadora (justificación Media como máximo).

##### 3.X.3.4. Mecanismo de Inferencia Difusa
1. **Fuzzificación:** Evaluación de los grados de pertenencia $\mu(C)$, $\mu(D)$ y $\mu(S)$ para los valores numéricos observados.
2. **Evaluación de Antecedentes (T-Norma MIN de Gödel):** El nivel de disparo o fuerza de activación de la regla $i$ ($w_i$) se calcula como la intersección de conjuntos difusos:
   $$w_i = \min(\mu_{C, i}(c), \mu_{D, i}(d), \mu_{S, i}(s)), \quad i = 1, \dots, 27$$
3. **Implicación Difusa (Corte de Consecuentes):** Para cada regla, la función de pertenencia de la salida se trunca mediante el operador mínimo:
   $$\mu_{y, i}'(y) = \min(w_i, \mu_{J, i}(y))$$
4. **Agregación de Reglas (S-Norma MAX):** La distribución difusa agregada $\mu_{\text{comb}}(y)$ en el universo de salida se obtiene por unión:
   $$\mu_{\text{comb}}(y) = \max_{i=1}^{27} \mu_{y, i}'(y)$$

Desglosando por el conjunto consecuente:
$$\text{activaAlta} = \max(w_{1}, w_{2}, w_{4}, w_{10}, w_{11}, w_{13})$$
$$\text{activaMedia} = \max(w_{3}, w_{5}, w_{7}, w_{12}, w_{14}, w_{16}, w_{19}, w_{20}, w_{22})$$
$$\text{activaBaja} = \max(w_{6}, w_{8}, w_{9}, w_{15}, w_{17}, w_{18}, w_{21}, w_{23}, w_{24}, w_{25}, w_{26}, w_{27})$$
$$\mu_{\text{comb}}(y) = \max \Big( \min(\text{activaAlta}, \mu_{\text{Alta}}(y)), \min(\text{activaMedia}, \mu_{\text{Media}}(y)), \min(\text{activaBaja}, \mu_{\text{Baja}}(y)) \Big)$$

##### 3.X.3.5. Defuzzificación por Centroide de Gravedad (COG)
Para transformar la distribución difusa $\mu_{\text{comb}}(y)$ en un valor escalar nítido $y^* \in [0.0, 1.0]$, se aplica el método del Centroide o Centro de Gravedad:
$$y^* = \frac{\int_0^1 y \cdot \mu_{\text{comb}}(y) \, dy}{\int_0^1 \mu_{\text{comb}}(y) \, dy}$$

En la implementación en Android (`JFuzzyEngine.kt`), la integral continua se resuelve numéricamente mediante el método de aproximación discreta de sumas de Riemann con $K = 100$ particiones ($\Delta y = 0.01$):
$$y^* \approx \frac{\sum_{k=0}^{100} y_k \cdot \mu_{\text{comb}}(y_k)}{\sum_{k=0}^{100} \mu_{\text{comb}}(y_k)}, \quad \text{donde } y_k = k \cdot 0.01$$

Si el denominador $\sum_{k=0}^{100} \mu_{\text{comb}}(y_k) \le 10^{-5}$ (ninguna regla disparada), se adopta $y^* = 0.0$.

---

### 3.Y. MÓDULO 4: GESTOR DE DICCIONARIOS ESCOLARES, EVALUACIÓN DE DECISIÓN Y PERSISTENCIA

El Módulo 4 administra los datos de vocabulario curricular del MINEDU, efectúa el juicio de corte nítido para la **sustitución silenciosa** y gestiona la persistencia local reactiva y la auditoría experimental.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        FLUJO COMPUTACIONAL DEL MÓDULO 4                                │
└────────────────────────────────────────────────────────────────────────────────────────┘
       y* provisto por Módulo 3
                  │
                  ▼
   ┌──────────────────────────────┐
   │ 4.1 DecisionEvaluator        │
   │  ¿ y* >= 0.65 ?              │
   └──────────────┬───────────────┘
                  ├────────────────────────────────────────┐
                  │ SÍ                                     │ NO
                  ▼                                        ▼
   ┌──────────────────────────────┐         ┌──────────────────────────────┐
   │ Sustitución Silenciosa       │         │ Conservar Término Original   │
   │ Preservación de mayúscula    │         │ (wasSubstituted = false)     │
   │ matchCase(original, canon)   │         └──────────────┬───────────────┘
   └──────────────┬───────────────┘                        │
                  └───────────────────┬────────────────────┘
                                      │
                                      ▼ WordCorrectionResult
   ┌───────────────────────────────────────────────────────────────────────┐
   │ 4.2 Persistencia Local & Caché (RoomDatabase: SQLite)                 │
   │  • AcademicTerm: Diccionarios escolares indexados (MINEDU)            │
   │  • Ingesta inicial desde assets JSON (Biología, Química, Historia...) │
   │  • Consultas ultrarrápidas DAO en RAM durante la sesión de clase     │
   └──────────────────────────────────┬────────────────────────────────────┘
                                      │
                                      ▼
   ┌───────────────────────────────────────────────────────────────────────┐
   │ 4.3 Telemetría y Auditoría Experimental (SessionLogManager)           │
   │  • Registro de latencia en milisegundos (t_fin - t_inicio)            │
   │  • Cálculo formal del Word Error Rate (WER) por dinámica de Levenshtein│
   │  • Auditoría persistente en SessionLogEntity                          │
   └───────────────────────────────────────────────────────────────────────┘
```

---

#### 3.Y.0. Ejemplo Práctico de Decisión, Persistencia en Room DB y Telemetría WER

Para ejemplificar la interacción entre el evaluador de decisión, la base de datos local y el cálculo de métricas de investigación, consideremos el procesamiento de la ráfaga de habla del **Caso de Estudio 1 (Biología)**:
- **Entrada ASR:** *"la cédula vegetal realiza la fotosintesis dentro del coroplasto y posee mitocondria"*
- **Resultados emitidos por el Módulo 3:**
  - *"cédula"* $\implies y^* = 0.785$
  - *"fotosintesis"* $\implies y^* = 0.836$
  - *"coroplasto"* $\implies y^* = 0.836$

**Ejecución en el Módulo 4:**
1. **Evaluación de Decisión y Formateo (`DecisionEvaluator`):**
   - Para *"cédula"*: Dado que $y^* = 0.785 \ge 0.65$ (y $D = 0.833 \ge 0.70$), se aprueba la sustitución silenciosa. Se invoca `matchCase("cédula", "célula")`. Como la palabra original iniciaba en minúscula, se genera `"célula"`.
   - Para *"coroplasto"*: Dado que $y^* = 0.836 \ge 0.65$, se genera `"cloroplasto"`.
   - Si el docente hubiera dicho *"Cédula vegetal..."* al inicio de la frase, `matchCase` habría producido automáticamente `"Célula"` con mayúscula, manteniendo la cohesión sintáctica del texto.
   - Oración final desplegada en el teleprompter accesible:  
     **"la célula vegetal realiza la fotosíntesis dentro del cloroplasto y posee mitocondria"**
2. **Persistencia Local Asíncrona (`RoomDatabase`):**
   - Durante la sesión, los 403 términos de Biología residen en la memoria RAM del dispositivo (`activeDictionary`), habiendo sido cargados desde la base de datos SQLite indexada. Esto permite que la búsqueda y cotejo tome menos de **15 milisegundos**, sin bloquear el hilo principal de la interfaz de usuario (UI Thread).
   - Por cada token corregido, se despacha en segundo plano (Coroutines `Dispatchers.IO`) un registro a la tabla `session_logs`:
     ```json
     {
       "sessionId": "a8f3b21c-4e89-4d2a-...",
       "subject": "BIOLOGIA",
       "timestamp": 1729428312000,
       "originalWord": "cédula",
       "correctedWord": "célula",
       "acousticConfidence": 0.42,
       "lexicalSimilarity": 0.833,
       "fuzzyJustification": 0.785,
       "substituted": true,
       "latencyMs": 7
     }
     ```
3. **Cálculo de Precisión Experimental (`SessionLogManager`):**
   - La verdad de campo consta de $N = 12$ palabras.
   - En el ASR crudo, se tenían 2 errores de sustitución (*"cédula"* por *"célula"*, *"coroplasto"* por *"cloroplasto"*), dando un $\text{WER}_{\text{crudo}} = \frac{2}{12} = 16.7\%$.
   - Con la intervención del Módulo 4, las sustituciones silenciosas eliminan dichos errores, reduciendo el error a $\text{WER}_{\text{difuso}} = 8.3\%$ (o $0.0\%$ con normalización de tildes), logrando una **reducción de error superior al 50%** y una precisión semántica final del **91.7%**, superando el umbral de hipótesis de la tesis ($\ge 90\%$).

---

#### 3.Y.1. Componente 4.1: Evaluador de Decisión y Sustitución Silenciosa (`DecisionEvaluator`)

##### 3.Y.1.1. Umbral Nítido de Decisión ($y^* \ge 0.65$)
El Grado de Justificación $y^*$ devuelto por el centroide difuso se contrasta frente a una constante de decisión crítica denominada $\tau_{\text{decisión}}$:
$$\tau_{\text{decisión}} = 0.65$$

La función de decisión $\delta(w_i, t_{\text{best}}, y^*)$ que determina la palabra final desplegada en el teleprompter se formaliza como:
$$\delta(w_i, t_{\text{best}}, y^*) = \begin{cases} 
\text{matchCase}(w_i, t_{\text{best}}), & \text{si } y^* \ge 0.65 \text{ y } D \ge 0.70 \text{ y } w_i \text{ no es flexión de } t_{\text{best}} \\
w_i, & \text{en otro caso}
\end{cases}$$

*Fundamentación del Umbral 0.65:*
El valor $\tau = 0.65$ es un parámetro de diseño calibrado empíricamente sobre el corpus de prueba. En el universo de salida, los conjuntos *Media* y *Alta* tienen igual pertenencia en $y^* = 0.675$; con $\tau = 0.65$ el umbral queda en la zona de transición ($\mu_{\text{Media}}(0.65) = 0.50$, $\mu_{\text{Alta}}(0.65) = 0.25$). Un umbral menor produce falsos positivos en el corpus de control (con $\tau \le 0.55$ aparece una sustitución indebida), mientras que uno mayor vuelve al sistema excesivamente conservador. Además de $y^* \ge \tau$, la implementación exige $D \ge 0.70$ y que la palabra no sea ya una flexión (singular/plural) del término, para evitar hipercorrecciones.

##### 3.Y.1.2. Algoritmo de Preservación Tipográfica (`matchCase`)
Para no perturbar la legibilidad ni la estructura gramatical (especialmente al inicio de oraciones o en nombres propios de próceres/científicos), la sustitución adapta la caja tipográfica del término canónico a la grafía de la palabra entrante:
$$\text{matchCase}(w_{\text{orig}}, t_{\text{canon}}) = \begin{cases} 
\text{capitalize}(t_{\text{canon}}), & \text{si } \text{isUpperCase}(w_{\text{orig}}[0]) = \text{true} \\
\text{toLowerCase}(t_{\text{canon}}), & \text{en otro caso}
\end{cases}$$

##### 3.Y.1.3. Estructura de Salida: `WordCorrectionResult`
El resultado de evaluar cada token se empaqueta en la estructura inmutable:
$$\text{WordCorrectionResult} = \langle w_{\text{orig}}, w_{\text{final}}, C, D, y^*, \beta_{\text{subst}} \rangle$$
- $w_{\text{orig}} \in \Sigma^*$: Palabra bruta generada por el ASR.
- $w_{\text{final}} \in \Sigma^*$: Palabra final aprobada para el teleprompter.
- $C \in [0.0, 1.0]$: Confianza acústica.
- $D \in [0.0, 1.0]$: Similitud combinada calculada.
- $y^* \in [0.0, 1.0]$: Justificación difusa.
- $\beta_{\text{subst}} \in \{\text{true}, \text{false}\}$: Bandera booleana que indica si ocurrió una sustitución silenciosa.

---

#### 3.Y.2. Componente 4.2: Modelo de Datos Relacional y Caché de Diccionarios (`RoomDatabase`)

##### 3.Y.2.1. Esquema Relacional de Entidades
La persistencia local se implementa sobre SQLite gestionado mediante la abstracción oficial **Android Jetpack Room 2.6.1**, optimizada mediante el procesador de símbolos **KSP** (*Kotlin Symbol Processing*).

###### Entidad `AcademicTerm` (Tabla `academic_terms`):
Representa cada término léxico curricular del MINEDU.
```kotlin
@Entity(
    tableName = "academic_terms",
    indices = [
        Index(value = ["term"]),
        Index(value = ["subject"])
    ]
)
data class AcademicTerm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val term: String,       // Forma canónica con tildes (ej. "mitocondria")
    val lemma: String,      // Lema base normalizado (ej. "mitocondria")
    val subject: String,    // Asignatura: "BIOLOGIA", "QUIMICA", "HISTORIA", "GEOGRAFIA"
    val category: String,   // Categoría gramatical: "NOUN", "PROPN"
    val phonetic: String    // Representación fonética precalculada
)
```
*Indexación:* Los índices en `term` y `subject` permiten que la consulta de carga al seleccionar la materia se ejecute en tiempo $O(K)$, donde $K$ es la cantidad de términos de dicha materia ($K \approx 200$), transfiriéndose íntegramente a una lista en memoria RAM (`activeDictionary`) para que el barrido durante el habla no toque el disco.

###### Entidad `SessionLogEntity` (Tabla `session_logs`):
Almacena el registro histórico detallado de cada palabra procesada para auditoría experimental y cálculo psicométrico y de WER:
```kotlin
@Entity(tableName = "session_logs")
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,          // UUID único de la sesión escolar de 90 min
    val subject: String,            // Materia activa
    val timestamp: Long,            // Marca de tiempo en milisegundos Unix
    val originalWord: String,       // Palabra original del ASR
    val correctedWord: String,      // Palabra final desplegada
    val acousticConfidence: Float,  // Variable C
    val lexicalSimilarity: Float,   // Variable D
    val fuzzyJustification: Float,  // Variable y*
    val substituted: Boolean,       // True si se aplicó corrección silenciosa
    val latencyMs: Long             // Tiempo de cómputo empleado en la ráfaga
)
```

##### 3.Y.2.2. Ingesta Inicial y Fallback desde Assets JSON
Para garantizar que la aplicación opere **inmediatamente después de ser instalada sin necesidad de conexión a internet**, la base de datos implementa una estrategia de carga desacoplada:
1. Al iniciar una sesión de clase, el ViewModel solicita al `DictionaryDao` los términos de la materia:
   ```sql
   SELECT * FROM academic_terms WHERE subject = :subject ORDER BY term ASC
   ```
2. Si la tabla retorna una lista vacía (primera ejecución en el dispositivo del estudiante), el sistema activa el cargador fallback (`loadFallbackDictionaryFromAssets`):
   - Lee el archivo JSON empaquetado en el APK: `assets/dictionaries/{materia}.json`.
   - Deserializa el contenido mediante `Gson` en una lista fuertemente tipada `List<AcademicTerm>`.
   - Inserta los términos en bloque mediante el DAO (`insertAll(terms)`) en una única transacción SQLite `@Insert(onConflict = OnConflictStrategy.REPLACE)`.
   - Retiene la lista en la variable en memoria `activeDictionary` durante toda la clase.

---

#### 3.Y.3. Componente 4.3: Gestor de Telemetría y Cálculo Formal de WER (`SessionLogManager`)

##### 3.Y.3.1. Telemetría de Latencia en Tiempo Real
El requerimiento de accesibilidad para estudiantes sordos exige que el subtitulado aparezca sincrónicamente con el habla del docente (latencia máxima admisible $< 1.0\text{ s}$). El gestor mide el tiempo de respuesta total del pipeline:
$$\Delta t_{\text{latencia}} = t_{\text{fin\_despliegue}} - t_{\text{inicio\_arribo\_asr}} \quad (\text{en milisegundos})$$
En las pruebas experimentales del prototipo, $\Delta t$ se situó consistentemente entre $4.5\text{ ms}$ y $55\text{ ms}$, consumiendo menos del 6% del presupuesto de tiempo real.

##### 3.Y.3.2. Formulación Matemática del Word Error Rate (WER)
Para la validación de la hipótesis de la tesis (reducir el WER por debajo del 10%), el sistema implementa el cálculo estricto del estándar de evaluación del habla de la NIST (*National Institute of Standards and Technology*):
$$\text{WER} = \frac{S + D + I}{N} = \frac{S + D + I}{S + D + C}$$

Donde:
- **$S$ (Sustituciones):** Palabras transcritas erróneamente en lugar de la palabra de referencia.
- **$D$ (Deleciones / Omisiones):** Palabras pronunciadas por el docente que el sistema omitió.
- **$I$ (Inserciones):** Palabras añadidas por la transcripción que nunca fueron emitidas.
- **$C$ (Aciertos):** Palabras transcritas con exacta coincidencia con la verdad de campo.
- **$N$:** Número total de palabras en la transcripción de referencia ($N = S + D + C$).

El cálculo de $S + D + I$ se efectúa a nivel de secuencia mediante alineamiento de secuencias de programación dinámica (distancia de Levenshtein a nivel de lista de palabras):
$$D_{\text{seq}}[i][j] = \min \begin{cases} 
D_{\text{seq}}[i-1][j-1] + \text{diff}(r_i, h_j) & \text{(Acierto o Sustitución)} \\
D_{\text{seq}}[i-1][j] + 1 & \text{(Deleción)} \\
D_{\text{seq}}[i][j-1] + 1 & \text{(Inserción)}
\end{cases}$$
Donde $r_i$ es el $i$-ésimo token de referencia y $h_j$ es el $j$-ésimo token de la hipótesis transcrita.

##### 3.Y.3.3. Estadísticas Agregadas de la Sesión (`SessionStats`)
Al término de cada clase escolar, `SessionLogManager` calcula las métricas globales consolidadas:
$$\text{SessionStats} = \langle N_{\text{palabras}}, N_{\text{sustituciones}}, \overline{\Delta t}_{\text{ms}}, \overline{C}, \overline{y}^* \rangle$$
- $\overline{\Delta t}_{\text{ms}} = \frac{1}{M} \sum_{k=1}^M \Delta t_k$ (Latencia media de inferencia).
- $\overline{C} = \frac{1}{N} \sum_{i=1}^N C_i$ (Confianza acústica media reportada por el ASR en la sesión. No mide el ruido del aula: el ruido se registra aparte, con sonómetro, en la ficha de observación).
- $\overline{y}^* = \frac{1}{N_{\text{subst}}} \sum_{j=1}^{N_{\text{subst}}} y^*_j$ (Grado de convicción difusa en las correcciones ejecutadas).

---

### 3.Z. TABLA MAESTRA DE VARIABLES, PARÁMETROS Y CONSTANTES

A continuación se consolidan formalmente todas las variables, símbolos matemáticos, rangos admisibles y roles funcionales que intervienen en los Módulos 3 y 4 del prototipo de tesis:

| Variable / Parámetro | Símbolo | Tipo de Dato | Rango / Dominio | Módulo / Clase | Descripción Técnica y Rol en la Tesis |
|:---|:---:|:---:|:---:|:---:|:---|
| **Palabra Bruta ASR** | $w_i$ | `String` | $\Sigma^*$ | Módulo 3 (`DecisionEvaluator`) | Token léxico entregado por la ráfaga continua de `SpeechRecognizer`. |
| **Confianza Acústica** | $C, C_i$ | `Double` | $[0.0, 1.0]$ | Módulo 3 (`JFuzzyEngine`) | Confianza por hipótesis (frase) reportada por `SpeechRecognizer`; 0.65 si no la informa. |
| **Palabra Normalizada** | $w_{\text{clean}}$ | `String` | $[a\text{-}z\text{áéíóúüñ}]^*$ | Módulo 3 (`OpenNlpPosTagger`) | Cadena sin signos diacríticos ni mayúsculas para filtrado gramatical. |
| **Distancia Levenshtein** | $L(s_1, s_2)$ | `Int` | $[0, \max(|s_1|, |s_2|)]$ | Módulo 3 (`LevenshteinMatcher`) | Número mínimo de operaciones de edición entre dos cadenas. |
| **Similitud Ortográfica** | $D_{\text{lex}}$ | `Double` | $[0.0, 1.0]$ | Módulo 3 (`LevenshteinMatcher`) | Cercanía de grafemas normalizada por la longitud máxima de palabra. |
| **Proyección Fonética** | $\Phi(s)$ | `String` | $\Sigma^*$ | Módulo 3 (`LevenshteinMatcher`) | Transformación fonológica dialectal hispana (seseo, yeísmo, b/v, h muda). |
| **Similitud Fonética** | $D_{\text{fon}}$ | `Double` | $[0.0, 1.0]$ | Módulo 3 (`LevenshteinMatcher`) | Distancia de edición calculada sobre las cadenas fonéticas neutralizadas. |
| **Factor de Ponderación** | $\alpha$ | `Double` | $0.60$ (constante) | Módulo 3 (`LevenshteinMatcher`) | Ponderador de combinación lineal: 60% ortográfico, 40% fonético. |
| **Similitud Combinada** | $D$ | `Double` | $[0.0, 1.0]$ | Módulo 3 (`LevenshteinMatcher`) | Entrada difusa 2: Métrica de proximidad léxico-fonética total. |
| **Coherencia Semántica** | $S$ | `Double` | $[0.0, 1.0]$ | Módulo 3 (`DecisionEvaluator`) | Entrada difusa 3: coherencia del candidato con los *topic clusters* de la oración y la memoria de la clase. |
| **Similitud Mínima de Sustitución** | $D_{\text{subst}}$ | `Double` | $0.70$ (constante) | Módulo 4 (`DecisionEvaluator`) | Piso adicional: sólo se sustituye si $D \ge 0.70$. |
| **Umbral Pre-activación** | $D_{\text{min}}$ | `Double` | $0.50$ (constante) | Módulo 3 (`DecisionEvaluator`) | Filtro de corte: si $D < 0.50$, no se invoca el motor difuso Mamdani. |
| **Conjunto Difuso Entrada** | $\mu_A(x)$ | Función | $[0.0, 1.0]$ | Módulo 3 (`MembershipFunctions`) | Grado de pertenencia trapezoidal o triangular para $C$, $D$ o $S$. |
| **Fuerza de Regla** | $w_k$ | `Double` | $[0.0, 1.0]$ | Módulo 3 (`JFuzzyEngine`) | Nivel de disparo de la regla difusa $k \in \{1,\dots,27\}$ mediante $\min(\mu_C, \mu_D, \mu_S)$. |
| **Distribución Agregada** | $\mu_{\text{comb}}(y)$ | Función | $[0.0, 1.0]$ | Módulo 3 (`JFuzzyEngine`) | Unión máxima ($\max$) de consecuentes truncados en el universo de justificación. |
| **Particiones de Riemann** | $K$ | `Int` | $100$ (constante) | Módulo 3 (`JFuzzyEngine`) | Número de pasos discretos para la integral numérica de defuzzificación COG. |
| **Grado de Justificación** | $y^*$ | `Double` | $[0.0, 1.0]$ | Módulo 3 / Módulo 4 | Valor escalar nítido obtenido por Centroide de Gravedad (COG). |
| **Umbral de Decisión** | $\tau_{\text{decisión}}$ | `Double` | $0.65$ (constante) | Módulo 4 (`DecisionEvaluator`) | Umbral nítido de la tesis: se sustituye si $y^* \ge 0.65$ y además $D \ge 0.70$. |
| **Bandera de Sustitución** | $\beta_{\text{subst}}$ | `Boolean` | $\{\text{true}, \text{false}\}$ | Módulo 4 (`WordCorrectionResult`) | Señala si la palabra fue reemplazada por la forma canónica escolar. |
| **Palabra Final Aprobada** | $w_{\text{final}}$ | `String` | $\Sigma^*$ | Módulo 4 (`DecisionEvaluator`) | Texto desplegado en el teleprompter accesible (WCAG AAA). |
| **Identificador de Sesión**| `sessionId` | `String` | UUID v4 | Módulo 4 (`SessionLogManager`) | Clave única de auditoría de la clase escolar de 45 a 90 minutos. |
| **Latencia de Inferencia** | $\Delta t$ | `Long` | $[0, \infty)\text{ ms}$ | Módulo 4 (`SessionLogManager`) | Tiempo transcurrido desde la llegada del ASR hasta el cálculo de decisión. |
| **Tasa de Error de Palabra**| $\text{WER}$ | `Double` | $[0.0, 1.0]$ | Módulo 4 (`SessionLogManager`) | Métrica formal de error de transcripción: $\text{WER} = (S + D + I) / N$. |

---

### CONCLUSIÓN DEL CAPÍTULO RESPECTO A ESTOS MÓDULOS
La articulación entre el **Módulo 3** (que modela matemáticamente la incertidumbre fonética del aula mediante lógica difusa Mamdani) y el **Módulo 4** (que garantiza la soberanía de datos locales, latencias inferiores a $15\text{ ms}$ mediante Room DB y la trazabilidad experimental rigurosa de WER) constituye la innovación metodológica central de esta tesis, permitiendo prescindir de la nube y democratizando el acceso pedagógico en tiempo real para estudiantes con discapacidad auditiva en el Perú.
