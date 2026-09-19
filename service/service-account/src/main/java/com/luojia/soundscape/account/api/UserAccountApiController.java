package com.luojia.soundscape.account.api;

import com.luojia.soundscape.account.service.UserAccountService;
import com.luojia.soundscape.common.login.SoundscapeLogin;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.common.util.AuthContextHolder;
import com.luojia.soundscape.vo.account.AccountDeductVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class UserAccountApiController {

	@Autowired
	private UserAccountService userAccountService;

	/**
	 * 查询当前用户账户可用金额
	 * @return 金额
	 */
	@SoundscapeLogin
	@Operation(summary = "查询当前用户账户可用金额")
	@GetMapping("/userAccount/getAvailableAmount")
	public Result<BigDecimal> getAvailableAmount(){
		Long userId = AuthContextHolder.getUserId();
		BigDecimal amount = userAccountService.getAvailableAmount(userId);
		return Result.ok(amount);
	}

	/**
	 * 检查且扣减账户金额
	 * @param accountDeductVo
	 * @return
	 */
	@Operation(summary = "检查且扣减账户金额")
	@PostMapping("/userAccount/checkAndDeduct")
	public Result checkAndDeduct(@RequestBody AccountDeductVo accountDeductVo){
		userAccountService.checkAndDeduct(accountDeductVo);
		return Result.ok();
	}
}

