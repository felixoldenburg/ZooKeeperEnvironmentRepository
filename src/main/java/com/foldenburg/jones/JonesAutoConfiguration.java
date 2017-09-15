package com.foldenburg.jones;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({JonesConfiguration.class})
public class JonesAutoConfiguration
{
}
