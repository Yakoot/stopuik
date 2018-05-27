<template>
  <div class="header">
    <div class="header-title">
      <div class="logo-row">
        <div class="first-title">РЕЕСТР НАРУШЕНИЙ</div>
        <img src="../assets/images/logo-head.png" srcset="../assets/images/logo-head@2x.png 2x, ../assets/images/logo-head@3x.png 3x" height="20">
      </div>
      <div class="second-title">Реестр нарушений избирательного законодательства членами УИК</div>
      <div class="third-title">Участковые избирательные коммисии 2011—2018</div>
    </div>
    <Region :regions="regions"
            :currentRegion="currentRegion"
            @change-region="changeRegion"/>
  </div>
</template>
<script>
  import Region from "./Region";

  export default {
    props: ["regions", "currentRegion"],
    components: {
      Region
    },
    data() {
      return {}
    },
    computed: {},
    methods: {
      changeRegion(region) {
        this.$emit("change-region", region)
      }
    }
  };
</script>
<style lang="sass">
  @import '../assets/style/theme.sass'
  div.header
    padding: 40px
    display: flex
    flex-direction: row
    justify-content: space-between

  .logo-row
    display: flex
    flex-direction: row
    img
      margin-left: 20px

  .header-title
    div
      text-align: left

  .first-title
    font-family: Lisa
    font-size: 31px
    color: $color-brick

  .second-title
    font-size: 18px
    color: black
    margin-top: 10px

  .third-title
    font-size: 12px
    color: $color-grey
</style>
