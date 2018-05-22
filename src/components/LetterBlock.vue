<template>
  <div class="letter-block">
    <div class="letter">{{ letter }}</div>
    <el-row :gutter="20">
      <el-col :span="8">
        <NameItem v-for="(item, index) in items.slice(0, partLength)" :key="index" :item="item"/>
      </el-col>
      <el-col :span="8">
        <NameItem v-for="(item, index) in items.slice(partLength, partLength*2)" :key="index" :item="item"/>
      </el-col>
      <el-col :span="8">
        <NameItem v-for="(item, index) in items.slice(partLength*2, partLength*3)" :key="index" :item="item"/>
      </el-col>
    </el-row>
  </div>
</template>
<script>

  import NameItem from './NameItem'
  export default {
    components: {NameItem},
    data() {
      return {

      }
    },
    props: ["letter", "items"],
    computed: {
      partLength() {
        return Math.ceil(this.items.length/3)
      }
    },
    methods: {
    }
  };
</script>
<style lang="sass">
  @import '../assets/style/theme.sass'

  .letter-block
    margin-top: 40px
    text-align: left
  .letter
    font-family: $open-sans
    font-size: 36px
    font-weight: 800

  .names
    column-count: 3
    margin-top: 20px

</style>
