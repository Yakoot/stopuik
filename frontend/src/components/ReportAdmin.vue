<template>
    <el-dialog
            title="Административный интерфейс"
            :visible.sync="visible"
            :before-close="handleClose"
            fullscreen
            center>
        <p>Этот интерфейс позволяет напрямую вносить изменения в базу данных.</p>
        <el-form ref="form" :model="form" label-width="120px">
            <el-form-item label="Номер УИК">
                <el-input v-model="form.uik" type="number"></el-input>
            </el-form-item>
            <el-form-item label="Член комиссии">
                <el-select
                        v-model="form.selectedUikMember"
                        :disabled="!uikMembersLoaded"
                        multiple
                        filterable
                        allow-create
                        default-first-option
                        placeholder="Кто нарушитель?">
                    <el-option
                            v-for="item in uikMembers"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value">
                    </el-option>
                </el-select>
            </el-form-item>
        </el-form>
        <el-form ref="form" :model="form" label-width="120px">
            <el-form-item>
                <el-button type="primary" @click="onSubmit">Создать запись</el-button>
                <el-button>Cancel</el-button>
            </el-form-item>
        </el-form>
    </el-dialog>
</template>
<script lang="ts">
  import {Component, Prop, Vue, Watch} from "vue-property-decorator";

  @Component({
  })
  export default class ReportAdmin extends Vue {
    @Prop(Boolean) readonly visible: boolean = true;

    private form = {
        uik: 0,
        selectedUikMember: undefined
    };
    private uikMembersLoaded = false;
    private uikMembers = [
        {
            value: 1,
            label: 'Иванов'
        },
        {
            value: 2,
            label: 'Петров'
        },
    ];
    close() {
        this.$emit("closeReport");
    }
    handleClose() {
        this.$emit("closeReport");
    }
    onSubmit() {
        console.log("!!!!!!!!!!!");
    }
    @Watch("form.uik")
    onUikNumberChange() {
      if (this.form.uik > 10) {
        this.uikMembersLoaded = true;
      }
    }
}
</script>
