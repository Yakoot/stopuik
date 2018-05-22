<template>
  <div class="registry-filter">
    <div v-for="item in letters" :key="item.letter" class="filter-item" :class="{ active: item.isActive }" @click="selectLetter(item)">{{ item.letter }}</div>
    <div class="filter-item flush" @click="flushFilter" v-if="letters.length > 0">Сбросить</div>
  </div>
</template>
<script>
  export default {
    data() {
      return {
        letters: []
      };
    },
    props: ["data"],
    computed: {},
    created() {
      this.createLetters()
    },
    watch: {
      "data": "createLetters"
    },
    methods: {
      selectLetter(item) {
        this.letters.forEach(item => item.isActive = false)
        item.isActive = true
        this.$emit("select-letter", item.letter)
      },
      flushFilter() {
        this.letters.forEach(item => item.isActive = false)
        this.$emit("select-letter", "")
      },
      createLetters() {
        this.data.forEach(letter => {
          this.letters.push({
            letter: letter,
            isActive: false
          })
        });
      }
    }
  };
</script>
<style lang="sass" scoped>
  @import '../assets/style/theme.sass'
  .registry-filter
    display: flex
    flex-flow: row wrap
  .filter-item
    height: 30px
    min-width: 30px
    font-size: 18px
    font-weight: bold
    background-color: #f7f7f7
    display: flex
    align-items: center
    justify-content: center
    cursor: pointer
    margin: 10px 6px 0 0
    &:hover
      background-color: $color-brick
      color: white
    &.active
      color: $color-brick
      background-color: unset
    &.flush
      padding: 0 10px
</style>
