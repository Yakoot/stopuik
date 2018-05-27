<template>
  <div class="registry-filter">
    <a v-for="item in letters" :key="item.letter" class="filter-item" :href="'#' + item.letter">{{ item.letter }}</a>
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
      createLetters() {
        this.data.forEach(letter => {
          this.letters.push({
            letter: letter
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
</style>
