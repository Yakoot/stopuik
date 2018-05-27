<template>
  <el-collapse-item class="person">
    <template slot="title">
      <div class="name-item">
        <div class="name">{{ item.name }}</div>
        <div class="uik">УИК {{ item.uik }}{{ item.uik_status ? `, ${item.uik_status}` : ""}}{{ years ? `, ${years}` : ""}}</div>
      </div>
    </template>
    <i class="el-icon-caret-top"></i>
    <div class="violations">
      <div class="violations-title">Нарушения</div>
      <el-collapse v-model="activeViolations">
        <div
            v-for="(violation, index) in violations"
            :key="index"
            class="violation">

          <el-collapse-item :name="index">
            <template slot="title">
              <div class="violation-description">{{ violation.description }}<i :class="activeViolations.includes(index) ? 'el-icon-caret-top' : 'el-icon-caret-bottom'"></i></div>
            </template>
            <div class="violation-link"
              v-for="(link_item, index) in violation.links"
              :key="index"
              v-if="link_item.link"><a :href="link_item.link" target="_blank">{{ link_item.link_description === "" ? "Источник" : link_item.link_description }}</a></div>
          </el-collapse-item>
        </div>
      </el-collapse>
    </div>
  </el-collapse-item>
</template>
<script>
  export default {
    data() {
      return {
        activeViolations: []
      }
    },
    props: ["item"],
    computed: {
      years() {
        if (this.item.years.length !== 0) {
          const yearStrings = this.item.years.map(year => {
            return `${year} г.`
          });
          return yearStrings.join(", ")
        }
        return ""
      },
      violations() {
        return this.item.violations.map(violation => {
          return {collapsed: true, ...violation}
        })
      }
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
    background-color: $color-brick !important


  .violations-title
    font-size: 14px
    color: rgba(255, 255, 255, 0.4)

  .person
    width: 300px
    margin-bottom: 20px !important
    > .el-collapse-item__wrap
      > .el-collapse-item__content
        position: relative
        padding-bottom: 25px
        margin-top: 10px
        color: white
        > .el-icon-caret-top
          color: $color-brick
          position: absolute
          top: -19px
          left: 20px
          font-size: 31px


  .violation
    margin-top: 15px
    font-size: 12px

    .el-collapse-item__wrap
      background-color: $color-brick !important

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
    padding-bottom: 0
    margin-top: 10px
</style>
