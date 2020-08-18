module.exports = {
    // options...
    devServer: {
        disableHostCheck: true
    },
    pages: {
      app: {
          entry: './src/main.ts',
          template: './public/index.html',
          filename: 'index.html',
      } ,
      auth: {
          entry: './src/auth.ts',
          template: './public/auth.html',
          filename: 'auth.html',
      },
      edit: {
          entry: './src/edit.ts',
          template: './public/edit.html',
          filename: 'edit.html',
      }
    }
}
