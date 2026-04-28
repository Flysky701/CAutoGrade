<template>
  <div class="code-editor-wrapper">
    <div ref="editorContainer" class="editor-container"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import * as monaco from 'monaco-editor'

const props = defineProps<{
  modelValue: string
  language?: string
  readOnly?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const editorContainer = ref<HTMLElement | null>(null)
let editor: monaco.editor.IStandaloneCodeEditor | null = null

onMounted(() => {
  if (!editorContainer.value) return
  editor = monaco.editor.create(editorContainer.value, {
    value: props.modelValue,
    language: props.language || 'c',
    theme: 'vs-dark',
    fontSize: 14,
    lineNumbers: 'on',
    minimap: { enabled: false },
    automaticLayout: true,
    readOnly: props.readOnly || false,
    scrollBeyondLastLine: false,
    tabSize: 4,
    insertSpaces: true,
  })

  editor.onDidChangeModelContent(() => {
    const value = editor!.getValue()
    emit('update:modelValue', value)
  })
})

watch(() => props.modelValue, (newVal) => {
  if (editor && editor.getValue() !== newVal) {
    editor.setValue(newVal)
  }
})

onBeforeUnmount(() => {
  editor?.dispose()
})
</script>

<style scoped>
.code-editor-wrapper {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}
.editor-container {
  height: 400px;
}
</style>
