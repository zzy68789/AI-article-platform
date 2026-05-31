package com.yupi.template.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yupi.template.model.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录 Mapper
 *
 * @author zzy
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
