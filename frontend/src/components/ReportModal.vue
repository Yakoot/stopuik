<template>
    <el-dialog
            title="Дополнить реестр"
            :visible.sync="visible"
            :before-close="handleClose"
            fullscreen
            center>
        <el-row type="flex" justify="end">
            <el-button type="text" @click="openAdminUi">Я админ</el-button>
        </el-row>
        <iframe src="https://docs.google.com/forms/d/e/1FAIpQLSf0uKaYdS8tcE-cn61WLMjrssR7wUHR0lZpeUbAJ2VzxWOUOQ/viewform?embedded=true" width="100%" height="700" frameborder="0" marginheight="0" marginwidth="0">Загрузка…</iframe>
    </el-dialog>
</template>
<script>
    export default {
        props: ["visible"],
        methods: {
            close() {
                this.$emit("closeReport");
            },
            handleClose() {
                this.$emit("closeReport");
            },
            openAdminUi() {
                this.$emit("openReportAdmin")
            }
        }
    };
</script>
