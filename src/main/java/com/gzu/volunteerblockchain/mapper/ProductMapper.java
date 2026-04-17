package com.gzu.volunteerblockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gzu.volunteerblockchain.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Update("""
        UPDATE products
        SET stock = stock - 1
        WHERE product_id = #{productId}
          AND stock > 0
        """)
    int decrementStock(@Param("productId") Integer productId);

    @Update("""
        UPDATE products
        SET stock = stock + 1
        WHERE product_id = #{productId}
        """)
    int incrementStock(@Param("productId") Integer productId);
}
