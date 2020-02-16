<template>
  <div>
    <mq-layout mq="sm">
      <div class="menu-sm">
        <el-menu
            :default-active="activeIndex"
            class="el-menu"
            mode="vertical"
            :router="true">
          <el-menu-item index="/registry">РЕЕСТР НАРУШЕНИЙ</el-menu-item>
          <el-menu-item index="/about">О ПРОЕКТЕ</el-menu-item>
        </el-menu>
        <el-button class="report" type="primary" @click="openReportModal">ДОПОЛНИТЬ РЕЕСТР</el-button>
      </div>
    </mq-layout>
    <mq-layout mq="md+">
      <div class="menu">
        <el-menu
            :default-active="activeIndex"
            class="el-menu"
            mode="horizontal"
            :router="true">
          <el-menu-item index="/registry">РЕЕСТР НАРУШЕНИЙ</el-menu-item>
          <el-menu-item index="/about">О ПРОЕКТЕ</el-menu-item>
        </el-menu>
        <el-button class="report" type="primary" @click="openReportModal">ДОПОЛНИТЬ РЕЕСТР</el-button>
      </div>
    </mq-layout>
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
        return this.$route.path === "/" ? "/registry" : this.$route.path
      }
    },
    methods: {
      openReportModal() {
        this.$emit("openReport")
      }
    }
  };
</script>
<style lang="scss" scoped>
  @import '../assets/style/theme';
  .menu-sm {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }
  .menu {
    display: flex;
    justify-content: space-between;
  }
  .el-menu {
    padding-left: 2%;
    background-color: #f7f7f7;
    flex-grow: 1;
    .el-menu-item {
      background-color: #f7f7f7;
      height: 50px;
      line-height: 50px;
      font-family: $open-sans;
      font-size: 14px;
      font-weight: bold;
      &.is-active {
        color: $color-brick
      }
    }
  }
  .report {
    font-family: $open-sans;
    font-size: 14px;
    font-weight: bold;
    color: white;
  }
</style>
