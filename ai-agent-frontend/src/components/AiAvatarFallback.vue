<template>
  <div class="ai-avatar-fallback" :class="type">
    <span v-if="type === 'love'">❤️</span>
    <span v-else-if="type === 'agent'" class="agent-icon">
      <span class="robot-head">
        <span class="robot-eye left"></span>
        <span class="robot-eye right"></span>
      </span>
    </span>
    <span v-else>🤖</span>
  </div>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'default'
  }
})
</script>

<style scoped>
.ai-avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  border-radius: 50%;
}

.love {
  background: linear-gradient(45deg, #ff6b8b, #ff8e8e);
}

.default, .super {
  background: linear-gradient(45deg, #3f51b5, #5677fc);
}

.agent {
  background: linear-gradient(135deg, #0d1c30 0%, #1a2d45 100%);
}

.agent-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.robot-head {
  width: 20px;
  height: 16px;
  background: linear-gradient(135deg, #cba659, #d4af37);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  position: relative;
}

.robot-head::before {
  content: '';
  position: absolute;
  top: -4px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 4px;
  background: #cba659;
  border-radius: 50%;
}

.robot-eye {
  width: 4px;
  height: 4px;
  background: #0d1c30;
  border-radius: 50%;
}
</style> 