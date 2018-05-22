<template>
  <div class="region-block">
    <div class="current-region">{{ currentRegion }}</div>
    <el-dropdown @command="changeRegion">
    <span class="el-dropdown-link">
      выбрать другой город
    </span>
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item
            v-for="region in regions"
            :key="region"
            :class="{ active: region === currentRegion }"
            :command="region">{{ region }}</el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
  </div>
</template>
<script>
  export default {
    data() {
      return {}
    },
    props: ["regions", "currentRegion"],
    computed: {},
    methods: {
      changeRegion(data) {
        this.$emit("change-region", data)
      }
    }
  };
</script>
<style lang="sass">
  @import '../assets/style/theme.sass'
  .region-block
    text-align: right
  .current-region
    font-size: 16px
    font-weight: bold
    line-height: 1.5

  .el-dropdown-link
    cursor: pointer
    color: $color-brick
    font-size: 11px

  .el-dropdown-menu__item.active
    background-color: #f2f2f2

</style>
