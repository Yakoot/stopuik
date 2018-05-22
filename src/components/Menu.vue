<template>
  <div class="menu">
    <el-menu
        :default-active="activeIndex"
        class="el-menu"
        mode="horizontal"
        :router="true">
      <el-menu-item index="/about">О ПРОЕКТЕ</el-menu-item>
      <el-menu-item index="/registry">РЕЕСТР НАРУШЕНИЙ</el-menu-item>
    </el-menu>
    <el-button class="report" type="primary" @click="openReportModal">СООБЩИТЬ О НАРУШЕНИИ</el-button>
  </div>
</template>
<script>
  export default {
    data() {
      return {
      };
    },
    computed: {
      activeIndex() {
        return this.$route.path === "/" ? "/about" : this.$route.path
      }
    },
    methods: {
      openReportModal() {
        this.$emit("openReport")
      }
    }
  };
</script>
<style lang="sass" scoped>
  @import '../assets/style/theme.sass'
  .menu
    display: flex
    flex-direction: row
    justify-content: space-between
  .el-menu
    padding-left: 40px
    background-color: #f7f7f7
    flex-grow: 1
    .el-menu-item
      background-color: #f7f7f7
      height: 50px
      line-height: 50px
      font:
        family: OpenSans
        size: 14px
        weight: bold
      letter-spacing: 1.4px
      &.is-active
        color: $color-brick
  .report
    font-family: $open-sans
    font-size: 14px
    font-weight: bold
    color: white
    letter-spacing: 1.4px
</style>
