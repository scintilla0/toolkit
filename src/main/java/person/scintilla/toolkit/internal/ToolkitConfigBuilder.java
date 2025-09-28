package person.scintilla.toolkit.internal;

public class ToolkitConfigBuilder {

	private final ToolkitConfig defaultToolkitConfig = new DefaultToolkitConfig();

	private String deleteFlagName = defaultToolkitConfig.getDeleteFlagName();
	private String defaultDateFormat = defaultToolkitConfig.getDefaultDateFormat();
	private String pageSizePropName = defaultToolkitConfig.getPageSizePropName();
	private String displayPageCountPropName = defaultToolkitConfig.getDisplayPageCountPropName();
	private String listEmptyErrorCode = defaultToolkitConfig.getListEmptyErrorCode();
	private String repeatErrorCode = defaultToolkitConfig.getRepeatErrorCode();
	private String emptyErrorCode = defaultToolkitConfig.getEmptyErrorCode();
	private String lengthErrorCode = defaultToolkitConfig.getLengthErrorCode();
	private String patternNumberErrorCode = defaultToolkitConfig.getPatternNumberErrorCode();
	private String patternAlphaNumErrorCode = defaultToolkitConfig.getPatternAlphaNumErrorCode();
	private String patternAlphaNumPuncErrorCode = defaultToolkitConfig.getPatternAlphaNumPuncErrorCode();

	public ToolkitConfigBuilder deleteFlagName(final String deleteFlagName) {
		this.deleteFlagName = deleteFlagName;
		return this;
	}

	public ToolkitConfigBuilder defaultDateFormat(String defaultDateFormat) {
		this.defaultDateFormat = defaultDateFormat;
		return this;
	}

	public ToolkitConfigBuilder pageSizePropName(String pageSizePropName) {
		this.pageSizePropName = pageSizePropName;
		return this;
	}

	public ToolkitConfigBuilder displayPageCountPropName(String displayPageCountPropName) {
		this.displayPageCountPropName = displayPageCountPropName;
		return this;
	}

	public ToolkitConfigBuilder listEmptyErrorCode(String listEmptyErrorCode) {
		this.listEmptyErrorCode = listEmptyErrorCode;
		return this;
	}

	public ToolkitConfigBuilder repeatErrorCode(String repeatErrorCode) {
		this.repeatErrorCode = repeatErrorCode;
		return this;
	}

	public ToolkitConfigBuilder emptyErrorCode(String emptyErrorCode) {
		this.emptyErrorCode = emptyErrorCode;
		return this;
	}

	public ToolkitConfigBuilder lengthErrorCode(String lengthErrorCode) {
		this.lengthErrorCode = lengthErrorCode;
		return this;
	}

	public ToolkitConfigBuilder patternNumberErrorCode(String patternNumberErrorCode) {
		this.patternNumberErrorCode = patternNumberErrorCode;
		return this;
	}

	public ToolkitConfigBuilder patternAlphaNumErrorCode(String patternAlphaNumErrorCode) {
		this.patternAlphaNumErrorCode = patternAlphaNumErrorCode;
		return this;
	}

	public ToolkitConfigBuilder patternAlphaNumPuncErrorCode(String patternAlphaNumPuncErrorCode) {
		this.patternAlphaNumPuncErrorCode = patternAlphaNumPuncErrorCode;
		return this;
	}

	public ToolkitConfig build() {
		return new ToolkitConfig() {

			@Override
			public String getDeleteFlagName() {
				return deleteFlagName;
			}

			@Override
			public String getDefaultDateFormat() {
				return defaultDateFormat;
			}

			@Override
			public String getPageSizePropName() {
				return pageSizePropName;
			}

			@Override
			public String getDisplayPageCountPropName() {
				return displayPageCountPropName;
			}

			@Override
			public String getListEmptyErrorCode() {
				return listEmptyErrorCode;
			}

			@Override
			public String getRepeatErrorCode() {
				return repeatErrorCode;
			}

			@Override
			public String getEmptyErrorCode() {
				return emptyErrorCode;
			}

			@Override
			public String getLengthErrorCode() {
				return lengthErrorCode;
			}

			@Override
			public String getPatternNumberErrorCode() {
				return patternNumberErrorCode;
			}

			@Override
			public String getPatternAlphaNumErrorCode() {
				return patternAlphaNumErrorCode;
			}

			@Override
			public String getPatternAlphaNumPuncErrorCode() {
				return patternAlphaNumPuncErrorCode;
			}
		};
	}

}