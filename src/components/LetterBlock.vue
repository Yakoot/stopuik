<template>
  <div class="letter-block">
    <a :name="letter"></a>
    <div class="letter">{{ letter }}</div>
    <mq-layout mq="xl">
      <el-row :gutter="40">
        <el-col :span="6">
          <NameItem v-for="(item, index) in items.slice(0, partLength(4))" :key="index" :item="item"/>
        </el-col>
        <el-col :span="6">
          <NameItem v-for="(item, index) in items.slice(partLength(4), partLength(4)*2)" :key="index" :item="item"/>
        </el-col>
        <el-col :span="6">
          <NameItem v-for="(item, index) in items.slice(partLength(4)*2, partLength(4)*3)" :key="index" :item="item"/>
        </el-col>
        <el-col :span="6">
          <NameItem v-for="(item, index) in items.slice(partLength(4)*3, partLength(4)*4)" :key="index" :item="item"/>
        </el-col>
      </el-row>
    </mq-layout>
    <mq-layout mq="lg">
      <el-row :gutter="40">
        <el-col :span="8">
          <NameItem v-for="(item, index) in items.slice(0, partLength(3))" :key="index" :item="item"/>
        </el-col>
        <el-col :span="8">
          <NameItem v-for="(item, index) in items.slice(partLength(3), partLength(3)*2)" :key="index" :item="item"/>
        </el-col>
        <el-col :span="8">
          <NameItem v-for="(item, index) in items.slice(partLength(3)*2, partLength(3)*3)" :key="index" :item="item"/>
        </el-col>
      </el-row>
    </mq-layout>
    <mq-layout mq="md">
      <el-row :gutter="40">
        <el-col :span="12">
          <NameItem v-for="(item, index) in items.slice(0, partLength(2))" :key="index" :item="item"/>
        </el-col>
        <el-col :span="12">
          <NameItem v-for="(item, index) in items.slice(partLength(2), partLength(2)*2)" :key="index" :item="item"/>
        </el-col>
      </el-row>
    </mq-layout>
    <mq-layout mq="sm">
      <el-row>
        <el-col :span="24">
          <NameItem v-for="(item, index) in items" :key="index" :item="item"/>
        </el-col>
      </el-row>
    </mq-layout>

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

    },
    methods: {
      partLength(count) {
        return Math.ceil(this.items.length/count)
      }
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
