# PT-BR Translation Audit - MobileCore / TuiMa

## Auditoria Inicial - Estado do Repositório

**Data**: 2026-08-18
**Versão upstream**: v0.1.4-rc9
**Repositório original**: https://github.com/Harzva/mobilecore

---

## 1. Estrutura do Projeto

### Arquivos Principais
- `android-app/` - Aplicativo Android
- `android-app/app/src/main/kotlin/ai/mobilecore/` - Código Kotlin principal
- `android-app/app/src/main/res/` - Recursos Android
- `android-app/app/build.gradle.kts` - Configuração Gradle

### Configuração do Build
| Configuração | Valor |
|--------------|-------|
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 1.9.24 |
| compileSdk | 35 |
| minSdk | 26 |
| targetSdk | 35 |
| NDK | 28.2.13676358 |
| CMake | 3.22.1 |
| JVM Target | 17 |
| ABI Filter | arm64-v8a |

---

## 2. Estado Atual da Internacionalização

### strings.xml Atual
**Localização**: `android-app/app/src/main/res/values/strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">TuiMa</string>
</resources>
```

**Apenas 1 string definida.** Todas as outras strings estão hardcoded diretamente nos arquivos Kotlin.

### Pastas de Localização
- `values/` - Inglês (padrão) - apenas `app_name`
- `values-zh/` - **NÃO EXISTE**
- `values-pt-rBR/` - **NÃO EXISTE**

### Status: ❌ SEM SUPORTE A LOCALIZAÇÃO

---

## 3. Arquivos com Strings Hardcoded

### Arquivos UI Principais
| Arquivo | Linhas | Strings Chinês | Localização |
|---------|--------|----------------|-------------|
| `MainActivity.kt` | ~5000+ | ~300+ | Textos hardcoded por toda a UI |
| `BenchmarkScreen.kt` | 99 | ~30 | Estados e mensagens de benchmark |
| `BenchmarkShareCardRenderer.kt` | 117 | ~20 | Labels de compartilhamento |
| `GallerySearchScreen.kt` | 542 | ~50 | Interface de busca |
| `GallerySearchState.kt` | 598 | ~60 | Estados e mensagens |
| `G2dValidationScreen.kt` | 915 | ~80 | Validação G2D |
| `OmniLifecycleScreen.kt` | 482 | ~40 | Ciclo de vida Omni |
| `PlaygroundPresenter.kt` | 266 | ~30 | Labels de playground |
| `ResultsScreen.kt` | 226 | ~20 | Tela de resultados |
| `HomeScreen.kt` | 87 | ~15 | Tela inicial |
| `VisionModelImportScreen.kt` | 663 | ~40 | Importação de modelos |
| `TuiMaComponents.kt` | 320 | ~10 | Componentes customizados |
| `TuiMaTheme.kt` | 78 | ~3 | Nomes de tema |
| `ModelLifecycleUi.kt` | 88 | ~8 | Estados do ciclo de vida |
| `MobileCoreService.kt` | ~150 | ~10 | Notificações |
| `DeviceProbe.kt` | ~100 | ~10 | Recomendações |

**Total estimado**: ~500+ strings hardcoded em chinês

---

## 4. Categorias de Strings Encontradas

### A. Navegação e Abas
- `端侧 AI 控制台` → Console de IA local
- `模型` → Modelos
- `模型广场` → Loja de Modelos
- `跑分` → Benchmark
- `结果` → Resultados
- `视觉实验室` → Laboratório Visual
- `本地相册搜索` → Busca Local na Galeria
- `视觉模型` → Modelo Visual
- `G2D 端侧验证` → Validação G2D Local
- `视觉` → Visual
- `本地多模态` → Multimodal Local
- `开发者接口` → Interface do Desenvolvedor
- `我的` → Meu Perfil

### B. Mensagens de Status
- `正在加载模型` → Carregando modelo
- `模型已加载` → Modelo carregado
- `模型加载失败` → Falha ao carregar modelo
- `准备就绪` → Pronto
- `跑分完成` → Benchmark concluído
- `模型已卸载` → Modelo descarregado

### C. Botões e Ações
- `下载` → Baixar
- `加载` → Carregar
- `暂停` → Pausar
- `继续` → Continuar
- `重新下载` → Baixar novamente
- `重试加载` → Tentar carregar novamente
- `选择图片` → Selecionar imagem
- `开始 OCR` → Iniciar OCR
- `导入模型` → Importar modelo
- `检查模型` → Verificar modelo

### D. Mensagens de Erro
- 13+ mensagens de falha na galeria
- Mensagens de memória insuficiente
- Mensagens de temperatura
- Mensagens de armazenamento

### E. Recomendações de Modelo
- 13 diferentes razões de recomendação para diferentes modelos

### F. Privacidade e Conformidade
- `离线运行 · 数据仅留本机` → Executa offline · Dados ficam apenas no dispositivo
- `MobileCore 不上传照片、查询或索引` → MobileCore não envia fotos, consultas ou índices
- `✓ 本机完成 · 原始内容不上传` → ✓ Concluído localmente · Conteúdo original não é enviado

---

## 5. Arquivos com Dados Técnicos (NÃO traduzir)

### Contratos de API
- `/v1/models`
- `/v1/chat/completions`
- `/health`
- `/metrics`
- `/mobilecore/model/load`
- `/mobilecore/model/unload`
- `model_id`
- `context_length`
- `speed`
- `stability`
- `Authorization`
- `Bearer local`

### Nomes de Modelos
- `Qwen2.5`
- `Qwen3`
- `Gemma`
- `local-model`
- Nomes de arquivos `.gguf`

### Identificadores
- `small`
- SHA-256 hashes
- URLs
- Nomes de provedores

---

## 6. Conclusão da Auditoria

### Status Atual
- ✅ Repositório clonado
- ✅ Branch `feat/pt-br-localization` criada
- ❌ Sem suporte a localização (apenas `app_name` em strings.xml)
- ❌ ~500+ strings hardcoded em chinês
- ❌ Nenhuma pasta de localização (values-zh, values-pt)

### Próximos Passos
1. Criar estrutura de internacionalização
2. Migrar strings hardcoded para resources
3. Criar tradução PT-BR
4. Configurar suporte automático ao idioma do sistema
5. Testar e gerar APK

---

## 7. Licença e Créditos

- **Autores**: Harzva (conforme repositório upstream)
- **Projeto original**: MobileCore / TuiMa
- **NÃO fazer rebranding** nesta fase
- **Manter** todos os créditos e referências ao projeto original
