package person.scintilla.toolkit.annotation;

public @interface FieldRule {

	String field();

	String name();

	ConstraintType constraint();

	String[] params() default {};

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public enum ConstraintType {

		/**
		 * @param format - String
		 * @param isRequired - boolean
		 */
		CHECK_TIME("checkTime"),
		/**
		 * @param format - String
		 * @param isRequired - boolean
		 */
		CHECK_DATE("checkDate"),
		/**
		 * @param isRequired - boolean
		 * @param maxIntegralLength - int
		 * @param maxFractionalLength - int
		 * @param allowMinus - boolean
		 */
		CHECK_DECIMAL("checkDecimal"),
		/**
		 * @param minLength - int
		 * @param maxLength - int
		 * @param allowMinus - boolean
		 */
		CHECK_NUMBER("checkNumber"),
		/**
		 * @param minLength - int
		 * @param maxLength - int
		 */
		CHECK_ALPHA_NUM("checkAlphaNum"),
		/**
		 * @param minLength - int
		 * @param maxLength - int
		 */
		CHECK_ALPHA_NUM_PUNC("checkAlphaNumPunc"),
		/**
		 * @param minLength - int
		 * @param maxLength - int
		 */
		CHECK_LENGTH("checkLength"),
		/**
		 * @param none
		 */
		CHECK_EMPTY("checkEmpty"),
		/**
		 * @param none
		 */
		CHECK_REPEAT("checkRepeat");

		////////////////////////////////////////////////////////////////////////////////////////////////////

		private final String constraintName;

		private ConstraintType(String constraintName) {
			this.constraintName = constraintName;
		}
		public String getConstraintName() {
			return constraintName;
		}

	}

}
