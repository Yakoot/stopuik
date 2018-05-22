<template>
  <el-collapse>
    <el-collapse-item>
      <template slot="title">
        <div class="name-item">
          <div class="name">{{ item.name }}</div>
          <div class="uik">УИК {{ item.uik }}{{ item.uik_status ? `, ${item.uik_status}` : ""}}</div>
        </div>
      </template>
      <i class="el-icon-caret-top"></i>
      <div class="violations">
        <div class="violations-title">Нарушения</div>
        <div
            v-for="(violation, index) in item.violations"
            :key="index"
            class="violation">
          <div class="violation-description">{{ violation.description }}</div>
          <div class="violation-link"
            v-for="(link_item, index) in violation.links"
            :key="index"
            v-if="link_item.link !== ''"><a :href="link_item.link" target="_blank">{{ link_item.link_description === "" ? "Источник" : link_item.link_description }}</a><i class="el-icon-caret-right"></i></div>
        </div>
      </div>
    </el-collapse-item>
  </el-collapse>

</template>
<script>
  export default {
    data() {
      return {}
    },
    props: ["item"],
    computed: {},
    methods: {
    }
  };
</script>
<style lang="sass">
  @import '../assets/style/theme.sass'

  .name-item
    line-height: normal
  .name
    color: $color-brick
    font-size: 16px
    line-height: 1.25
  .uik
    color: #9b9b9b
    font-size: 12px
    line-height: 1.5

  .violations
    padding: 20px

  .violations-title
    font-size: 14px
    color: rgba(255, 255, 255, 0.4)

  .violation
    margin-top: 20px
    font-size: 12px
    .violation-description
      font-weight: bold
      color: #fff

    .violation-link
      margin: 10px 0 0 20px
      color: #fff
      a
        color: #fff
        cursor: pointer
        &:hover
          border-bottom: 1px solid
  .el-collapse
    border: none !important
  .el-collapse-item
    margin-bottom: 20px !important
  .el-collapse-item__header
    height: unset !important
    line-height: unset
    background-color: unset
    color: inherit
    cursor: pointer
    border-bottom: none !important
    font-size: unset
    font-weight: unset
    transition: unset
  .el-collapse-item__wrap
    border: none
  .el-collapse-item__arrow
    display: none !important
  .el-collapse-item__content
    position: relative
    margin-top: 10px
    background-color: $color-brick
    color: white
  .el-icon-caret-top
    color: $color-brick
    position: absolute
    top: -19px
    left: 20px
    font-size: 31px
</style>
