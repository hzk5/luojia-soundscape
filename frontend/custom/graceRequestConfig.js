// grace reuqest 请求库相关配置

export default{
	apiBaseUrl        : 'http://127.0.0.1:8500/api',
	debug             : true,
	expiredTime       : 3600,
	postHeaderDefault : 'application/json',
	getToken          : function(){
		return new Promise((resolve, reject) => {
			resolve()
		});
	},
	writeLoginToken : function (token, uid){
		var loginToken = token+'-'+uid;
		uni.setStorageSync(this.userTokenKeyName, loginToken);
		return ;
	},
	tokenErrorMessage : function(){
		uni.showToast({
			title : "请求失败, 请重试",
			icon  : "none"
		})
	}
}
