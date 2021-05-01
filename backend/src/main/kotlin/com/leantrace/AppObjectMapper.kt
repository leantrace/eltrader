import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.text.SimpleDateFormat

/**
 *
 *
 * Created on 30.04.21
 * @author: Alexander Schamne <alexander.schamne@gmail.com>
 *
 */
class AppObjectMapper : ObjectMapper {

    constructor() : super()
    private constructor(mapper: ObjectMapper) : super(mapper)

    init {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        disable(FAIL_ON_UNKNOWN_PROPERTIES)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        disable(WRITE_DATES_AS_TIMESTAMPS)
        dateFormat = SimpleDateFormat(DATE_FORMAT)
    }

    override fun copy(): ObjectMapper {
        return AppObjectMapper(this)
    }

    companion object {

        const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
        private val MAPPER: AppObjectMapper = AppObjectMapper()

        fun mapper(): AppObjectMapper {
            return MAPPER;
        }
    }
}
